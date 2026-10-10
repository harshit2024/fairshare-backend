package com.fairShare.fairshare_backend.users;

import com.fairShare.fairshare_backend.users.dto.CreateUserRequest;
import com.fairShare.fairshare_backend.users.dto.UpdateUserRequest;
import com.fairShare.fairshare_backend.users.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deleteUser(UUID id);
}
