package com.cloudfilemanager.user;

import com.cloudfilemanager.email.EmailService;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Long tokenExpirationHours;

    @Autowired
    public EmailVerificationService(
            UserRepository userRepository,
            EmailService emailService,
            @Value("${app.email.verification.token-expiration-hours:24}") Long tokenExpirationHours) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.tokenExpirationHours = tokenExpirationHours;
    }

    @Transactional
    public void generateAndSendVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(tokenExpirationHours);
        
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenExpiresAt(expirationTime);
        userRepository.save(user);
        
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (user.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification token has expired");
        }

        if (user.isEnabled()) {
            throw new IllegalStateException("Email is already verified");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = userRepository.verifyEmail(token, now);
        
        if (updated == 0) {
            throw new ResourceNotFoundException("Could not verify email");
        }
    }

    @Transactional
    public void resendVerificationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.isEnabled()) {
            throw new IllegalStateException("Email is already verified");
        }

        generateAndSendVerificationToken(user);
    }
}
