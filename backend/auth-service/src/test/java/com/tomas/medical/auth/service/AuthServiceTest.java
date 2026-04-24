package com.tomas.medical.auth.service;

import com.tomas.medical.auth.dto.request.LoginRequest;
import com.tomas.medical.auth.dto.request.RegisterRequest;
import com.tomas.medical.auth.dto.request.UpdateUserProfileRequest;
import com.tomas.medical.auth.dto.response.AuthResponse;
import com.tomas.medical.auth.dto.response.InternalUserResponse;
import com.tomas.medical.auth.dto.response.UserMeResponse;
import com.tomas.medical.auth.entity.Role;
import com.tomas.medical.auth.entity.RoleName;
import com.tomas.medical.auth.entity.User;
import com.tomas.medical.auth.exception.EmailAlreadyExistsException;
import com.tomas.medical.auth.exception.IncompletePasswordUpdateException;
import com.tomas.medical.auth.exception.InvalidCurrentPasswordException;
import com.tomas.medical.auth.mapper.UserMapper;
import com.tomas.medical.auth.repository.RoleRepository;
import com.tomas.medical.auth.repository.UserRepository;
import com.tomas.medical.auth.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private AuthService authService;

    @Test
    void registerPatientCreatesUserWithPatientRole() {
        RegisterRequest request = new RegisterRequest("Alice Patient", "alice@example.com", "Password123");
        Role role = role(RoleName.PATIENT);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.PATIENT)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserMeResponse response = authService.registerPatient(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo("PATIENT");
    }

    @Test
    void registerDoctorFailsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Dr Bob", "bob@example.com", "Password123");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerDoctor(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginReturnsJwtTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "Password123");
        Role role = role(RoleName.PATIENT);
        User user = new User();
        user.setEmail("alice@example.com");
        user.setRole(role);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("alice@example.com", null));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice@example.com", Map.of("role", "PATIENT"))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void getInternalUserByEmailReturnsFullName() {
        Role role = role(RoleName.PATIENT);
        User user = new User();
        user.setId(10L);
        user.setFullName("Alice Patient");
        user.setEmail("alice@example.com");
        user.setRole(role);
        user.setActive(true);

        when(userRepository.findByEmailAndActiveTrue("alice@example.com")).thenReturn(Optional.of(user));

        InternalUserResponse response = authService.getInternalUserByEmail("Alice@Example.com");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.fullName()).isEqualTo("Alice Patient");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void getInternalUserByIdReturnsFullName() {
        Role role = role(RoleName.DOCTOR);
        User user = new User();
        user.setId(77L);
        user.setFullName("Dr Bob");
        user.setEmail("bob@example.com");
        user.setRole(role);
        user.setActive(true);

        when(userRepository.findByIdAndActiveTrue(77L)).thenReturn(Optional.of(user));

        InternalUserResponse response = authService.getInternalUserById(77L);

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.fullName()).isEqualTo("Dr Bob");
        assertThat(response.role()).isEqualTo("DOCTOR");
    }

    @Test
    void updateMeChangesFullNameWithoutPasswordChange() {
        User user = activeUser("alice@example.com", "old-hash", role(RoleName.PATIENT));
        user.setFullName("Alice Original");

        when(userRepository.findByEmailAndActiveTrue("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserMeResponse response = authService.updateMe(
                "Alice@Example.com",
                new UpdateUserProfileRequest("Alice Updated", null, null)
        );

        assertThat(response.fullName()).isEqualTo("Alice Updated");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void updateMeChangesPasswordWhenCurrentPasswordMatches() {
        User user = activeUser("alice@example.com", "old-hash", role(RoleName.PATIENT));
        user.setFullName("Alice Original");

        when(userRepository.findByEmailAndActiveTrue("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword456")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserMeResponse response = authService.updateMe(
                "alice@example.com",
                new UpdateUserProfileRequest("Alice Updated", "Password123", "NewPassword456")
        );

        assertThat(response.fullName()).isEqualTo("Alice Updated");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void updateMeThrowsWhenCurrentPasswordIsInvalid() {
        User user = activeUser("alice@example.com", "old-hash", role(RoleName.PATIENT));

        when(userRepository.findByEmailAndActiveTrue("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.updateMe(
                "alice@example.com",
                new UpdateUserProfileRequest("Alice Updated", "bad-password", "NewPassword456")
        )).isInstanceOf(InvalidCurrentPasswordException.class);
    }

    @Test
    void updateMeThrowsWhenPasswordUpdateIsIncomplete() {
        User user = activeUser("alice@example.com", "old-hash", role(RoleName.PATIENT));

        when(userRepository.findByEmailAndActiveTrue("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.updateMe(
                "alice@example.com",
                new UpdateUserProfileRequest("Alice Updated", "Password123", null)
        )).isInstanceOf(IncompletePasswordUpdateException.class);
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setId(1L);
        role.setName(roleName);
        return role;
    }

    private User activeUser(String email, String passwordHash, Role role) {
        User user = new User();
        user.setId(99L);
        user.setFullName("Default Name");
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
