package com.tomas.medical.auth.mapper;

import com.tomas.medical.auth.dto.response.UserMeResponse;
import com.tomas.medical.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserMeResponse toMeResponse(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName().name()
        );
    }
}
