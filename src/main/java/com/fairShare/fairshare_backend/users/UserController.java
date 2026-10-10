package com.fairShare.fairshare_backend.users;
import com.fairShare.fairshare_backend.users.dto.CreateUserRequest;
import com.fairShare.fairshare_backend.users.dto.UpdateUserRequest;
import com.fairShare.fairshare_backend.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /users
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResponse response =
                userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET /users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID id
    ) {

        UserResponse response =
                userService.getUserById(id);

        return ResponseEntity.ok(response);
    }

    // GET /users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        List<UserResponse> users =
                userService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    // PUT /users/{id}
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UserResponse response =
                userService.updateUser(id, request);

        return ResponseEntity.ok(response);
    }

    // DELETE /users/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id
    ) {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
