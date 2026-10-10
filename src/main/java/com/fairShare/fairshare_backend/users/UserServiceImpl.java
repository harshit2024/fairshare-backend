package com.fairShare.fairshare_backend.users;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.fairShare.fairshare_backend.users.dto.CreateUserRequest;
import com.fairShare.fairshare_backend.users.dto.UpdateUserRequest;
import com.fairShare.fairshare_backend.users.dto.UserResponse;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists with email: " + email);
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);

        // Never store plain text passwords
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id: " + id)
                );
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Override
    public UserResponse updateUser(
            UUID id,
            UpdateUserRequest request
    ) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id: " + id)
                );

        if (request.getName() != null &&
                !request.getName().isBlank()) {

            user.setName(request.getName().trim());
        }

        if (request.getEmail() != null &&
                !request.getEmail().isBlank()) {

            String newEmail =
                    request.getEmail().trim().toLowerCase();

            if (!newEmail.equals(user.getEmail())
                    && userRepository.existsByEmail(newEmail)) {

                throw new RuntimeException(
                        "Email already exists: " + newEmail
                );
            }

            user.setEmail(newEmail);
        }

        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(
                            request.getPassword()
                    )
            );
        }

        User updatedUser = userRepository.save(user);

        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException(
                    "User not found with id: " + id
            );
        }
        userRepository.deleteById(id);
    }
}
