package com.cloudfilemanager.user;

import com.cloudfilemanager.user.dto.UserDto;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class UserDtoMapper implements Function<User, UserDto> {

    @Override
    public UserDto apply(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt()
        );
    }
}
