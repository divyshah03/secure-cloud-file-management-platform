package com.cloudfilemanager.auth.dto;

import com.cloudfilemanager.user.dto.UserDTO;
public record AuthenticationResponse(
        String token,
        UserDTO user
) {
}
