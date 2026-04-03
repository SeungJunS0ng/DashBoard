package com.festapp.dashboar.service;

import com.festapp.dashboar.dto.UserInfoResponse;
import com.festapp.dashboar.entity.User;
import com.festapp.dashboar.exception.ResourceNotFoundException;
import com.festapp.dashboar.repository.RefreshTokenRepository;
import com.festapp.dashboar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserInfoResponse getUserById(Long userId) {
        log.debug("Fetching user by userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        return convertToDTO(user);
    }

    public UserInfoResponse getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new ResourceNotFoundException("User not found");
                });

        return convertToDTO(user);
    }

    public List<UserInfoResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public UserInfoResponse updateUser(Long userId, UserInfoResponse updateRequest) {
        log.info("Updating user with userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        if (updateRequest.getFullName() != null) {
            user.setFullName(updateRequest.getFullName());
            log.debug("Updated fullName for userId: {}", userId);
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with userId: {}", userId);
        return convertToDTO(updatedUser);
    }

    @Transactional
    public void activateUser(Long userId) {
        log.info("Activating user with userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User activated successfully with userId: {}", userId);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Deactivating user with userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
        user.setIsActive(false);
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);
        log.info("User deactivated successfully with userId: {}", userId);
    }

    @Transactional
    public void promoteToAdmin(Long userId) {
        log.info("Promoting user to ADMIN with userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
        log.info("User promoted to ADMIN successfully with userId: {}", userId);
    }

    @Transactional
    public void demoteToUser(Long userId) {
        log.info("Demoting user to USER role with userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
        user.setRole(User.Role.USER);
        userRepository.save(user);
        log.info("User demoted to USER role successfully with userId: {}", userId);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user with userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        // Delete all refresh tokens associated with this user
        refreshTokenRepository.deleteByUser(user);

        // Delete the user
        userRepository.delete(user);
        log.info("User deleted successfully with userId: {}", userId);
    }

    private UserInfoResponse convertToDTO(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().toString())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}



