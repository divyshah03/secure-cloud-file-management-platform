package com.cloudfilemanager.auth;

import com.cloudfilemanager.user.EmailVerificationService;
import com.cloudfilemanager.user.UserService;
import com.cloudfilemanager.auth.dto.AuthenticationRequest;
import com.cloudfilemanager.auth.dto.AuthenticationResponse;
import com.cloudfilemanager.auth.dto.EmailVerificationRequest;
import com.cloudfilemanager.user.dto.UserRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            AuthenticationService authenticationService,
            UserService userService,
            EmailVerificationService emailVerificationService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please check your email for verification link."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authenticationService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, response.token())
                .body(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmailPost(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.verifyEmail(request.token());
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationToken(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
        }
        
        emailVerificationService.resendVerificationToken(email);
        return ResponseEntity.ok(Map.of("message", "Verification email sent successfully"));
    }
}
