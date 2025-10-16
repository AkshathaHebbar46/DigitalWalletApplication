package org.transactions.digitalwallettraining.service;

import org.springframework.stereotype.Service;
import org.transactions.digitalwallettraining.dto.UserRequestDTO;
import org.transactions.digitalwallettraining.dto.UserResponseDTO;
import org.transactions.digitalwallettraining.entity.UserEntity;
import org.transactions.digitalwallettraining.mapper.UserMapper;
import org.transactions.digitalwallettraining.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper = UserMapper.INSTANCE;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Create a new user
    public UserResponseDTO createUser(UserRequestDTO request) {
        UserEntity user = mapper.toEntity(request);
        UserEntity saved = userRepository.save(user);
        return mapper.toDTO(saved);
    }

    // Get all users
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get user by ID
    public UserResponseDTO getUserById(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return mapper.toDTO(user);
    }

    // Update user
    public UserResponseDTO updateUser(Long userId, UserRequestDTO request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setName(request.name());
        user.setEmail(request.email());
        UserEntity updated = userRepository.save(user);
        return mapper.toDTO(updated);
    }

    // Delete user
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
