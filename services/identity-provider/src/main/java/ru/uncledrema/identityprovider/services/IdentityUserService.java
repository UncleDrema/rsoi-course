package ru.uncledrema.identityprovider.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.uncledrema.identityprovider.dto.CreateUserRequest;
import ru.uncledrema.identityprovider.dto.UserDto;
import ru.uncledrema.identityprovider.types.IdentityRole;
import ru.uncledrema.identityprovider.types.IdentityUser;

import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class IdentityUserService {
    private final IdentityUserRepository identityUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return identityUserRepository.findAll(Sort.by(Sort.Direction.ASC, "username")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        identityUserRepository.findByUsernameIgnoreCase(request.username())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(CONFLICT, "Username already exists");
                });

        IdentityUser user = new IdentityUser();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(IdentityRole.USER));

        return toDto(identityUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public IdentityUser getRequiredUser(String username) {
        return identityUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    @Transactional
    public IdentityUser seedAdminUser(String username, String email, String name, String password) {
        IdentityUser user = identityUserRepository.findByUsernameIgnoreCase(username).orElseGet(IdentityUser::new);
        user.setUsername(username);
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of(IdentityRole.ADMIN));
        return identityUserRepository.save(user);
    }

    private UserDto toDto(IdentityUser user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getName(), user.getRoles());
    }
}
