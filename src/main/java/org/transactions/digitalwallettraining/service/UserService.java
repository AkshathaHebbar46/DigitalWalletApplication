package org.transactions.digitalwallettraining.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.dto.UserRequestDTO;
import org.transactions.digitalwallettraining.dto.UserResponseDTO;
import org.transactions.digitalwallettraining.entity.UserEntity;
import org.transactions.digitalwallettraining.entity.WalletEntity;
import org.transactions.digitalwallettraining.mapper.UserMapper;
import org.transactions.digitalwallettraining.repository.UserRepository;
import org.transactions.digitalwallettraining.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UserMapper mapper = UserMapper.INSTANCE;

    // ✅ Constructor injection for both repositories
    public UserService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * ✅ Creates a new user and automatically assigns a default wallet with balance = 0.0
     */
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO request) {
        log.info("Creating new user with name={} and email={}", request.name(), request.email());

        // Create and save user
        UserEntity user = new UserEntity(request.name(), request.email(), request.age());
        userRepository.save(user);

        // 🪙 Automatically create a default wallet with balance = 0.0
        WalletEntity wallet = new WalletEntity(user, 0.0);
        walletRepository.save(wallet);

        log.info("✅ User created successfully (userId={}), default wallet created (walletId={}, balance={})",
                user.getId(), wallet.getId(), wallet.getBalance());

        // Return response DTO
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }

    // ✅ Get all users
    public List<UserResponseDTO> getAllUsers() {
        log.info("Fetching all users from database");
        List<UserResponseDTO> users = userRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
        log.debug("Total users found: {}", users.size());
        return users;
    }

    // ✅ Get user by ID
    public UserResponseDTO getUserById(Long userId) {
        log.info("Fetching user with ID: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new RuntimeException("User not found: " + userId);
                });
        return mapper.toDTO(user);
    }

    // ✅ Update user
    public UserResponseDTO updateUser(Long userId, UserRequestDTO request) {
        log.info("Updating user with ID: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for update: {}", userId);
                    return new RuntimeException("User not found: " + userId);
                });

        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        UserEntity updated = userRepository.save(user);

        log.debug("User updated successfully: {}", updated);
        return mapper.toDTO(updated);
    }

    // ✅ Delete user
    public void deleteUser(Long userId) {
        log.info("Attempting to delete user with ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.error("User not found for deletion: {}", userId);
            throw new RuntimeException("User not found: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("User deleted successfully: {}", userId);
    }
}
