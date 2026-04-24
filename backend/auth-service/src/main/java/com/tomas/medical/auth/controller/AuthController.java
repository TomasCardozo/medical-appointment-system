package com.tomas.medical.auth.controller;

import com.tomas.medical.auth.dto.request.LoginRequest;
import com.tomas.medical.auth.dto.request.RegisterRequest;
import com.tomas.medical.auth.dto.request.UpdateUserProfileRequest;
import com.tomas.medical.auth.dto.response.AuthResponse;
import com.tomas.medical.auth.dto.response.UserMeResponse;
import com.tomas.medical.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/patient")
    public ResponseEntity<UserMeResponse> registerPatient(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPatient(request));
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<UserMeResponse> registerDoctor(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerDoctor(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserMeResponse> updateMe(@Valid @RequestBody UpdateUserProfileRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(authService.updateMe(authentication.getName(), request));
    }

}
