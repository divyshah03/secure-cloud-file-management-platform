package com.cloudfilemanager.email;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String userName, String token);
}
