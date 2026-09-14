package com.cloudfilemanager.auth;

import com.cloudfilemanager.user.UserDTOMapper;
import com.cloudfilemanager.user.User;
import com.cloudfilemanager.security.JWTUtil;
import com.cloudfilemanager.auth.dto.AuthenticationRequest;
import com.cloudfilemanager.auth.dto.AuthenticationResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDTOMapper userDTOMapper;
    private final JWTUtil jwtUtil;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserDTOMapper userDTOMapper,
            JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDTOMapper = userDTOMapper;
        this.jwtUtil = jwtUtil;
    }

    public AuthenticationResponse login(AuthenticationRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            User principal = (User) authentication.getPrincipal();
            
            if (!principal.isEnabled()) {
                throw new IllegalStateException("Email not verified. Please verify your email before logging in.");
            }

            com.cloudfilemanager.user.dto.UserDTO userDTO = userDTOMapper.apply(principal);
            String token = jwtUtil.issueToken(userDTO.email(), userDTO.role().name());
            
            return new AuthenticationResponse(token, userDTO);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
