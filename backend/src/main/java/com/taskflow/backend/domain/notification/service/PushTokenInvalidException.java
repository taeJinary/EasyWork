package com.taskflow.backend.domain.notification.service;

public class PushTokenInvalidException extends RuntimeException {

    public PushTokenInvalidException(String message) {
        super(message);
    }
}
