package com.cloudfilemanager.auth.dto;

import com.cloudfilemanager.user.dto.UserDto;
public record AuthenticationResponse(
        String token,
        UserDto user
) {
}
