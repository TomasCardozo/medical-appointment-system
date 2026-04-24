package com.tomas.medical.auth.security.user;

import com.tomas.medical.auth.entity.Role;
import com.tomas.medical.auth.entity.RoleName;
import com.tomas.medical.auth.entity.User;
import com.tomas.medical.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameUsesFindByEmailWithRoleAndBuildsAuthorities() {
        User user = user("alice@example.com", true, RoleName.PATIENT);
        when(userRepository.findByEmailWithRole("alice@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("alice@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("alice@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("hashed-password");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PATIENT");

        verify(userRepository).findByEmailWithRole("alice@example.com");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsernameReturnsDisabledWhenUserIsInactive() {
        User inactiveUser = user("inactive@example.com", false, RoleName.DOCTOR);
        when(userRepository.findByEmailWithRole("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("inactive@example.com");

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_DOCTOR");
    }

    @Test
    void loadUserByUsernameThrowsWhenUserDoesNotExist() {
        when(userRepository.findByEmailWithRole("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@example.com");

        verify(userRepository).findByEmailWithRole("missing@example.com");
    }

    private User user(String email, boolean active, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
