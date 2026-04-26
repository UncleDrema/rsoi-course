package ru.uncledrema.identityprovider.services;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.uncledrema.identityprovider.config.IdentityProviderProperties;

@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements ApplicationRunner {
    private final IdentityProviderProperties properties;
    private final IdentityUserService identityUserService;

    @Override
    public void run(ApplicationArguments args) {
        IdentityProviderProperties.Admin admin = properties.getAdmin();
        identityUserService.seedAdminUser(
                admin.getUsername(),
                admin.getEmail(),
                admin.getName(),
                admin.getPassword()
        );
    }
}
