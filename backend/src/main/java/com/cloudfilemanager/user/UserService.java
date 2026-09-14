package com.cloudfilemanager.user;

import com.cloudfilemanager.user.dto.UserDTO;
import com.cloudfilemanager.user.dto.UserRegistrationRequest;
import com.cloudfilemanager.common.exception.DuplicateResourceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserDTOMapper userDTOMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public UserService(
            UserRepository userRepository,
            UserDTOMapper userDTOMapper,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.userDTOMapper = userDTOMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public UserDTO registerUser(UserRegistrationRequest request) {
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

        return userDTOMapper.apply(savedUser);
    }

    public UserDTO getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userDTOMapper)
                .orElseThrow(() -> new com.cloudfilemanager.common.exception.ResourceNotFoundException(
                        "User not found with email: " + email));
    }

    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(userDTOMapper)
                .orElseThrow(() -> new com.cloudfilemanager.common.exception.ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.cloudfilemanager.common.exception.ResourceNotFoundException(
                        "User not found with email: " + email));
    }
}
