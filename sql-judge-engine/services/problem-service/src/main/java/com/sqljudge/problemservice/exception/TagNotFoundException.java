package com.sqljudge.problemservice.exception;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(Long tagId) {
        super("Tag not found with id: " + tagId);
    }

    public TagNotFoundException(String message) {
        super(message);
    }
}