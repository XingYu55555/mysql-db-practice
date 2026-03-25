package com.sqljudge.problemservice.exception;

public class ProblemNotFoundException extends RuntimeException {
    public ProblemNotFoundException(Long problemId) {
        super("Problem not found with id: " + problemId);
    }

    public ProblemNotFoundException(String message) {
        super(message);
    }
}