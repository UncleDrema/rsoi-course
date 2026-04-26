package ru.uncledrema.identityprovider.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import ru.uncledrema.identityprovider.config.IdentityProviderProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminUserSeederTest {
    @Test
    void seedsConfiguredAdminUserOnStartup() throws Exception {
        IdentityProviderProperties properties = new IdentityProviderProperties();
        properties.getAdmin().setUsername("admin");
        properties.getAdmin().setEmail("admin@example.com");
        properties.getAdmin().setName("Admin User");
        properties.getAdmin().setPassword("secret");

        IdentityUserService service = mock(IdentityUserService.class);
        AdminUserSeeder seeder = new AdminUserSeeder(properties, service);

        seeder.run(new DefaultApplicationArguments(new String[0]));

        verify(service).seedAdminUser("admin", "admin@example.com", "Admin User", "secret");
    }
}
