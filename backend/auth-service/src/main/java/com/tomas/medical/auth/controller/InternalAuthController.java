package com.tomas.medical.auth.controller;

import com.tomas.medical.auth.dto.response.InternalUserResponse;
import com.tomas.medical.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalAuthController {

    private final AuthService authService;

    public InternalAuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/by-email")
    public ResponseEntity<InternalUserResponse> getInternalUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(authService.getInternalUserByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternalUserResponse> getInternalUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getInternalUserById(id));
    }
}
