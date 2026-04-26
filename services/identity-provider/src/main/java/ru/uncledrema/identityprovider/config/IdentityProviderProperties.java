package ru.uncledrema.identityprovider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity-provider")
public class IdentityProviderProperties {
    private String issuer = "http://localhost:8090";
    private final Cors cors = new Cors();
    private final Signing signing = new Signing();
    private final Admin admin = new Admin();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Signing getSigning() {
        return signing;
    }

    public Cors getCors() {
        return cors;
    }

    public Admin getAdmin() {
        return admin;
    }

    public static class Cors {
        private String allowedOrigins = "http://127.0.0.1:3000,http://localhost:3000";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Signing {
        private String privateKeyPem = "";
        private String publicKeyPem = "";

        public String getPrivateKeyPem() {
            return privateKeyPem;
        }

        public void setPrivateKeyPem(String privateKeyPem) {
            this.privateKeyPem = privateKeyPem;
        }

        public String getPublicKeyPem() {
            return publicKeyPem;
        }

        public void setPublicKeyPem(String publicKeyPem) {
            this.publicKeyPem = publicKeyPem;
        }
    }

    public static class Admin {
        private String username = "dev-admin";
        private String password = "dev-admin-password";
        private String email = "dev-admin@example.com";
        private String name = "Development Admin";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
