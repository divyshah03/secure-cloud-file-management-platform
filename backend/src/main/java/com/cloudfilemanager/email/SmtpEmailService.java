package com.cloudfilemanager.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@org.springframework.context.annotation.Primary
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true", matchIfMissing = true)
public class SmtpEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String baseUrl;
    private final boolean emailEnabled;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.email.from:noreply@filemanager.com}") String fromEmail,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.email.enabled:true}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
        this.emailEnabled = emailEnabled;
    }

    public void sendVerificationEmail(String toEmail, String userName, String token) {
        if (!emailEnabled) {
            logger.info("Email sending is disabled. Verification token for {}: {}", toEmail, token);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify Your Email Address");
            
            String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;
            String emailBody = buildVerificationEmailBody(userName, verificationUrl, token);
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String buildVerificationEmailBody(String userName, String verificationUrl, String token) {
        return String.format("""
                Hello %s,
                
                Thank you for registering with Cloud File Manager!
                
                Please verify your email address by clicking the link below:
                %s
                
                Or copy and paste this token in the verification form:
                %s
                
                This link will expire in 24 hours.
                
                If you did not create an account, please ignore this email.
                
                Best regards,
                Cloud File Manager Team
                """, userName, verificationUrl, token);
    }
}
