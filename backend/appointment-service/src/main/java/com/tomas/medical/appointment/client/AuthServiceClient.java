package com.tomas.medical.appointment.client;

import com.tomas.medical.appointment.client.dto.InternalUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/internal/users/by-email")
    InternalUserResponse getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/internal/users/{id}")
    InternalUserResponse getUserById(@PathVariable("id") Long id);
}
