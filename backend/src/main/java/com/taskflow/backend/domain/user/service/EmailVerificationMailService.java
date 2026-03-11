package com.taskflow.backend.domain.user.service;

public interface EmailVerificationMailService {

    boolean isReady();

    void sendVerificationEmail(String recipientEmail, String rawToken);
}
