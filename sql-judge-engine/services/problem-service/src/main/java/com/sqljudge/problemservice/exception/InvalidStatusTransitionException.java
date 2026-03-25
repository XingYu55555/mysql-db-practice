package com.sqljudge.problemservice.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super("Invalid status transition from " + currentStatus + " to " + targetStatus);
    }
}