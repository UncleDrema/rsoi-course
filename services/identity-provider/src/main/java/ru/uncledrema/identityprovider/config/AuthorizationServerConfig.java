package ru.uncledrema.identityprovider.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import ru.uncledrema.identityprovider.services.IdentityUserService;
import ru.uncledrema.identityprovider.types.IdentityUser;

import java.time.Duration;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    private final IdentityProviderProperties properties;
    private final IdentityUserService identityUserService;

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = buildRsaKey(properties.getSigning());
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(properties.getIssuer())
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            @Value("${IDENTITY_PROVIDER_PUBLIC_CLIENT_ID:spa-client}") String publicClientId,
            @Value("${IDENTITY_PROVIDER_PUBLIC_REDIRECT_URI_1:http://127.0.0.1:3000/auth/callback}") String redirectUri1,
            @Value("${IDENTITY_PROVIDER_PUBLIC_REDIRECT_URI_2:http://localhost:3000/auth/callback}") String redirectUri2,
            @Value("${IDENTITY_PROVIDER_PUBLIC_POST_LOGOUT_REDIRECT_URI_1:http://127.0.0.1:3000/login}") String postLogoutRedirectUri1,
            @Value("${IDENTITY_PROVIDER_PUBLIC_POST_LOGOUT_REDIRECT_URI_2:http://localhost:3000/login}") String postLogoutRedirectUri2,
            @Value("${IDENTITY_PROVIDER_ACCESS_TOKEN_TTL:PT1H}") Duration accessTokenTimeToLive
    ) {
        RegisteredClient publicSpa = RegisteredClient.withId("public-spa")
                .clientId(publicClientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri1)
                .redirectUri(redirectUri2)
                .postLogoutRedirectUri(postLogoutRedirectUri1)
                .postLogoutRedirectUri(postLogoutRedirectUri2)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(accessTokenTimeToLive)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(publicSpa);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            IdentityUser user = identityUserService.getRequiredUser(context.getPrincipal().getName());
            Set<String> roles = new LinkedHashSet<>(user.getRoles().stream().map(Enum::name).toList());
            String tokenType = context.getTokenType() == null ? "" : context.getTokenType().getValue();

            if (OAuth2TokenType.ACCESS_TOKEN.getValue().equals(tokenType)
                    || OidcParameterNames.ID_TOKEN.equals(tokenType)) {
                applyUserClaims(context, user, roles);
            }
        };
    }

    private void applyUserClaims(JwtEncodingContext context, IdentityUser user, Set<String> roles) {
        context.getClaims().subject(user.getUsername());
        context.getClaims().claim("preferred_username", user.getUsername());
        context.getClaims().claim("name", user.getName());
        context.getClaims().claim("email", user.getEmail());
        context.getClaims().claim("roles", roles);
    }

    private RSAKey buildRsaKey(IdentityProviderProperties.Signing signing) {
        try {
            if (hasText(signing.getPrivateKeyPem())) {
                RSAPrivateKey privateKey = readPrivateKey(signing.getPrivateKeyPem());
                RSAPublicKey publicKey = hasText(signing.getPublicKeyPem())
                        ? readPublicKey(signing.getPublicKeyPem())
                        : derivePublicKey(privateKey);
                return new RSAKey.Builder(publicKey)
                        .privateKey(privateKey)
                        .keyID(UUID.randomUUID().toString())
                        .build();
            }
            log.warn("Identity provider signing key PEM is not configured. Generating an ephemeral RSA key for development.");
            KeyPair keyPair = generateRsaKey();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to initialize signing key", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private RSAPrivateKey readPrivateKey(String pem) throws GeneralSecurityException {
        byte[] content = decodePem(pem, "PRIVATE KEY");
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(content));
        return (RSAPrivateKey) privateKey;
    }

    private RSAPublicKey readPublicKey(String pem) throws GeneralSecurityException {
        byte[] content = decodePem(pem, "PUBLIC KEY");
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(content));
        return (RSAPublicKey) publicKey;
    }

    private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) throws GeneralSecurityException {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalStateException("Public key PEM is required when the private key does not expose CRT parameters");
        }
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
    }

    private byte[] decodePem(String pem, String type) {
        String sanitized = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(sanitized);
    }

    private KeyPair generateRsaKey() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
