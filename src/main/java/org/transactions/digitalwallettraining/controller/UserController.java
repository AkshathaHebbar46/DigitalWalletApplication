package org.transactions.digitalwallettraining.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.transactions.digitalwallettraining.dto.UserRequestDTO;
import org.transactions.digitalwallettraining.dto.UserResponseDTO;
import org.transactions.digitalwallettraining.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Create a new user
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
        logger.info("Creating new user with email={}", request.email());

        UserResponseDTO response = userService.createUser(request);

        logger.info("User created successfully with id={}", response.id());
        return ResponseEntity.status(201).body(response);
    }

    // Get all users
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        logger.info("Fetching all users");

        List<UserResponseDTO> users = userService.getAllUsers();

        logger.debug("Number of users retrieved: {}", users.size());
        return ResponseEntity.ok(users);
    }

    // Get user by ID
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long userId) {
        logger.info("Fetching user by id={}", userId);

        UserResponseDTO response = userService.getUserById(userId);

        logger.debug("User fetched: id={}, email={}", response.id(), response.email());
        return ResponseEntity.ok(response);
    }

    // Updathne user
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long userId, @RequestBody UserRequestDTO request) {
        logger.info("Updating user id={} with new data: email={}, name={}, age={}", userId, request.email(), request.name(), request.age());

        UserResponseDTO response = userService.updateUser(userId, request);

        logger.info("User updated successfully: id={}", response.id());
        return ResponseEntity.ok(response);
    }

    // Delete user
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        logger.info("Deleting user with id={}", userId);

        userService.deleteUser(userId);

        logger.info("User deleted successfully: id={}", userId);
        return ResponseEntity.noContent().build();
    }
}
