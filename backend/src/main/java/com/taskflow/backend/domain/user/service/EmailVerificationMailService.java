package com.taskflow.backend.domain.user.service;

public interface EmailVerificationMailService {

    void sendVerificationEmail(String recipientEmail, String rawToken);
}
