package com.cloudfilemanager.user;

import com.cloudfilemanager.user.dto.UserDto;
import com.cloudfilemanager.user.dto.UserRegistrationRequest;
import com.cloudfilemanager.common.exception.DuplicateResourceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public UserService(
            UserRepository userRepository,
            UserDtoMapper userDtoMapper,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.userDtoMapper = userDtoMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public UserDto registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already taken: " + request.email());
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.USER);
        user.setEnabled(false);

        User savedUser = userRepository.save(user);
        emailVerificationService.generateAndSendVerificationToken(savedUser);

        return userDtoMapper.apply(savedUser);
    }

    public UserDto getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userDtoMapper)
                .orElseThrow(() -> new com.cloudfilemanager.common.exception.ResourceNotFoundException(
                        "User not found with email: " + email));
    }

    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(userDtoMapper)
                .orElseThrow(() -> new com.cloudfilemanager.common.exception.ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.cloudfilemanager.common.exception.ResourceNotFoundException(
                        "User not found with email: " + email));
    }
}
