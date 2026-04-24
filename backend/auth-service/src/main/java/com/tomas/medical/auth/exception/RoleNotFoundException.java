package com.tomas.medical.auth.exception;

import com.tomas.medical.auth.entity.RoleName;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(RoleName roleName) {
        super("Role not found: " + roleName);
    }
}
