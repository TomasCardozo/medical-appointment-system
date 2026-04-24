package com.tomas.medical.auth.exception;

public class IncompletePasswordUpdateException extends RuntimeException {
    public IncompletePasswordUpdateException() {
        super("currentPassword and newPassword are required to change password");
    }
}
