package com.cloudfilemanager.auth;

import com.cloudfilemanager.user.UserDtoMapper;
import com.cloudfilemanager.user.User;
import com.cloudfilemanager.security.JwtUtil;
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
    private final UserDtoMapper userDtoMapper;
    private final JwtUtil jwtUtil;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserDtoMapper userDtoMapper,
            JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDtoMapper = userDtoMapper;
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

            com.cloudfilemanager.user.dto.UserDto userDto = userDtoMapper.apply(principal);
            String token = jwtUtil.issueToken(userDto.email(), userDto.role().name());

            return new AuthenticationResponse(token, userDto);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
