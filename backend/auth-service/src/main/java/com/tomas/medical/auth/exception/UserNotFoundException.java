package com.tomas.medical.auth.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String email) {
        super("User not found for email: " + email);
    }
}
