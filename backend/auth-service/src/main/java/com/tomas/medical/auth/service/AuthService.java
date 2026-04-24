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
import com.tomas.medical.auth.exception.InvalidCredentialsException;
import com.tomas.medical.auth.exception.RoleNotFoundException;
import com.tomas.medical.auth.exception.UserNotFoundException;
import com.tomas.medical.auth.mapper.UserMapper;
import com.tomas.medical.auth.repository.RoleRepository;
import com.tomas.medical.auth.repository.UserRepository;
import com.tomas.medical.auth.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserMeResponse registerPatient(RegisterRequest request) {
        return registerWithRole(request, RoleName.PATIENT);
    }

    @Transactional
    public UserMeResponse registerDoctor(RegisterRequest request) {
        return registerWithRole(request, RoleName.DOCTOR);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateToken(user.getEmail(), Map.of("role", user.getRole().getName().name()));
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs() / 1000);
    }

    @Transactional(readOnly = true)
    public UserMeResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        return userMapper.toMeResponse(user);
    }

    @Transactional
    public UserMeResponse updateMe(String email, UpdateUserProfileRequest request) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailAndActiveTrue(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));

        user.setFullName(request.fullName());

        boolean hasCurrentPassword = hasText(request.currentPassword());
        boolean hasNewPassword = hasText(request.newPassword());

        if (hasCurrentPassword != hasNewPassword) {
            throw new IncompletePasswordUpdateException();
        }

        if (hasCurrentPassword) {
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new InvalidCurrentPasswordException();
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        User saved = userRepository.save(user);
        return userMapper.toMeResponse(saved);
    }

    @Transactional(readOnly = true)
    public InternalUserResponse getInternalUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailAndActiveTrue(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));

        return toInternalUserResponse(user);
    }

    @Transactional(readOnly = true)
    public InternalUserResponse getInternalUserById(Long id) {
        User user = userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(id)));

        return toInternalUserResponse(user);
    }

    private UserMeResponse registerWithRole(RegisterRequest request, RoleName roleName) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(roleName));

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);

        User saved = userRepository.save(user);
        return userMapper.toMeResponse(saved);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private InternalUserResponse toInternalUserResponse(User user) {
        return new InternalUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName().name(),
                user.isActive()
        );
    }
}
