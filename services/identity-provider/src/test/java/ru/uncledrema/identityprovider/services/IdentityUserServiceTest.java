package ru.uncledrema.identityprovider.services;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.uncledrema.identityprovider.dto.CreateUserRequest;
import ru.uncledrema.identityprovider.dto.UserDto;
import ru.uncledrema.identityprovider.types.IdentityRole;
import ru.uncledrema.identityprovider.types.IdentityUser;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityUserServiceTest {
    @Test
    void createUserAssignsUserRoleAndEncodesPassword() {
        IdentityUserRepository repository = mock(IdentityUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(repository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(repository.save(any(IdentityUser.class))).thenAnswer(invocation -> {
            IdentityUser user = invocation.getArgument(0, IdentityUser.class);
            return user;
        });

        IdentityUserService service = new IdentityUserService(repository, passwordEncoder);
        UserDto created = service.createUser(new CreateUserRequest("alice", "alice@example.com", "Alice", "raw-password"));

        assertThat(created.username()).isEqualTo("alice");
        assertThat(created.email()).isEqualTo("alice@example.com");
        assertThat(created.name()).isEqualTo("Alice");
        assertThat(created.roles()).isEqualTo(Set.of(IdentityRole.USER));
    }
}
