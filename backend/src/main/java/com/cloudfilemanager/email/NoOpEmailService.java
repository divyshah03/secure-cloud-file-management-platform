package com.cloudfilemanager.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = false)
public class NoOpEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpEmailService.class);
    
    private final String baseUrl;

    public NoOpEmailService(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String userName, String token) {
        String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        logger.info("Email sending is disabled. Verification token for {}: {}", toEmail, token);
        logger.info("Verification URL: {}", verificationUrl);
    }
}
