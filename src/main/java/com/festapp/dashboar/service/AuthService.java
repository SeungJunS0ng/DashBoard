package com.festapp.dashboar.service;

import com.festapp.dashboar.dto.*;
import com.festapp.dashboar.entity.RefreshToken;
import com.festapp.dashboar.entity.User;
import com.festapp.dashboar.exception.AuthException;
import com.festapp.dashboar.exception.ResourceNotFoundException;
import com.festapp.dashboar.exception.ValidationException;
import com.festapp.dashboar.repository.RefreshTokenRepository;
import com.festapp.dashboar.repository.UserRepository;
import com.festapp.dashboar.security.CustomUserDetails;
import com.festapp.dashboar.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserInfoResponse signUp(SignUpRequest request) {
        log.info("Attempting to sign up user with username: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Sign up failed: Username already exists - {}", request.getUsername());
            throw new AuthException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Sign up failed: Email already exists - {}", request.getEmail());
            throw new AuthException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User signed up successfully with userId: {} and username: {}", savedUser.getUserId(), savedUser.getUsername());

        return UserInfoResponse.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().toString())
                .isActive(savedUser.getIsActive())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Attempting to login user: {}", request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Check if user is active
            if (!userDetails.isEnabled()) {
                log.warn("Login failed: User is inactive - {}", request.getUsername());
                throw new AuthException("User account is inactive");
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(userDetails.getUserId());

            // Get user for additional info
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Save refresh token
            long refreshTokenExpirationMs = 604800000; // 7 days
            LocalDateTime expiryDate = LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);

            RefreshToken savedRefreshToken = RefreshToken.builder()
                    .user(user)
                    .token(refreshToken)
                    .expiresAt(expiryDate)
                    .isRevoked(false)
                    .build();

            refreshTokenRepository.save(savedRefreshToken);
            log.info("Login successful for user: {} (userId: {})", user.getUsername(), user.getUserId());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProvider.getAccessTokenExpirationTime())
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .role(user.getRole().toString())
                    .build();
        } catch (AuthException | ResourceNotFoundException e) {
            log.warn("Login failed for user: {} - {}", request.getUsername(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Login failed for user: {} - Invalid credentials", request.getUsername());
            throw new AuthException("Invalid username or password");
        }
    }

    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        log.info("Attempting to refresh access token");
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            log.warn("Token refresh failed: Invalid or expired refresh token");
            throw new AuthException("Invalid or expired refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthException("Refresh token not found"));

        if (storedToken.getIsRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Token refresh failed: Refresh token has been revoked or expired");
            throw new AuthException("Refresh token has been revoked or expired");
        }

        Long userId = jwtProvider.getUserIdFromJWT(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtProvider.generateAccessTokenFromUserId(
                user.getUserId(),
                user.getUsername(),
                user.getRole().toString()
        );

        log.info("Access token refreshed for user: {} (userId: {})", user.getUsername(), user.getUserId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationTime())
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole().toString())
                .build();
    }

    @Transactional
    public void logout() {
        log.info("Attempting to logout user");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            refreshTokenRepository.deleteByUser(user);
            log.info("User logged out successfully: {} (userId: {})", user.getUsername(), user.getUserId());
        }
    }

    public UserInfoResponse getUserInfo(Long userId) {
        log.debug("Retrieving user info for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Attempting to change password for userId: {}", userId);

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Change password failed for userId: {} - Password confirmation mismatch", userId);
            throw new ValidationException("New password and confirm password do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Change password failed for userId: {} - Current password incorrect", userId);
            throw new AuthException("Current password is incorrect");
        }

        // Check if new password is same as current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("Change password failed for userId: {} - New password is same as current", userId);
            throw new ValidationException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user: {} (userId: {})", user.getUsername(), user.getUserId());
    }
}


