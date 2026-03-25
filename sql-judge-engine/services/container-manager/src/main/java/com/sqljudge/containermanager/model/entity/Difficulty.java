package com.sqljudge.containermanager.model.entity;

public enum Difficulty {
    EASY(10),
    MEDIUM(30),
    HARD(60);

    private final int timeoutMinutes;

    Difficulty(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }
}