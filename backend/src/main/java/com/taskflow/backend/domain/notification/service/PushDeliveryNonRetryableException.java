package com.taskflow.backend.domain.notification.service;

public class PushDeliveryNonRetryableException extends RuntimeException {

    public PushDeliveryNonRetryableException(String message) {
        super(message);
    }

    public PushDeliveryNonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
