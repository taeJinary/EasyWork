package com.taskflow.backend.domain.notification.service;

public class PushDeliveryRetryableException extends RuntimeException {

    public PushDeliveryRetryableException(String message) {
        super(message);
    }

    public PushDeliveryRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
