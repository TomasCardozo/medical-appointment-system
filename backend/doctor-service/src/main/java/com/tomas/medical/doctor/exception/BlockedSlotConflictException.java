package com.tomas.medical.doctor.exception;

public class BlockedSlotConflictException extends RuntimeException {

    public BlockedSlotConflictException(String message) {
        super(message);
    }
}
