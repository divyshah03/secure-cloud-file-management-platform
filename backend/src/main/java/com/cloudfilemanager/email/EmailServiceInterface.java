package com.cloudfilemanager.email;

public interface EmailServiceInterface {
    void sendVerificationEmail(String toEmail, String userName, String token);
}
