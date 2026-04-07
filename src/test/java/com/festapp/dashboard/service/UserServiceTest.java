// 사용자 서비스 테스트 - UserService의 비즈니스 로직 검증
package com.festapp.dashboard.service;

import com.festapp.dashboard.user.dto.UserCreateRequest;
import com.festapp.dashboard.user.dto.UserInfoResponse;
import com.festapp.dashboard.user.dto.UserUpdateRequest;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import com.festapp.dashboard.user.service.UserService;
import com.festapp.dashboard.auth.repository.RefreshTokenRepository;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword123")
                .fullName("Test User")
                .role(User.Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("사용자 생성 성공")
    void testCreateUserSuccess() {
        // Given
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .role("USER")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(User.builder()
                .userId(2L)
                .username("newuser")
                .email("newuser@example.com")
                .password("hashedPassword")
                .fullName("New User")
                .role(User.Role.USER)
                .isActive(true)
                .build());

        // When
        UserInfoResponse response = userService.createUser(createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 생성 실패 - 중복된 사용자명")
    void testCreateUserFailureDuplicateUsername() {
        // Given
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .username("testuser")
                .email("newuser@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .role("USER")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 생성 실패 - 중복된 이메일")
    void testCreateUserFailureDuplicateEmail() {
        // Given
        UserCreateRequest createRequest = UserCreateRequest.builder()
                .username("newuser")
                .email("test@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .role("USER")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 ID로 사용자 조회 성공")
    void testGetUserByIdSuccess() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserInfoResponse response = userService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("사용자 ID로 사용자 조회 실패 - 존재하지 않는 사용자")
    void testGetUserByIdNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("사용자명으로 사용자 조회 성공")
    void testGetUserByUsernameSuccess() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserInfoResponse response = userService.getUserByUsername("testuser");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - fullName만 수정")
    void testUpdateUserSuccessFullNameOnly() {
        // Given
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserInfoResponse response = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - 이메일 변경")
    void testUpdateUserSuccessEmailChange() {
        // Given
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .email("newemail@example.com")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserInfoResponse response = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository, times(1)).existsByEmail("newemail@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 이메일 중복")
    void testUpdateUserFailureEmailDuplicate() {
        // Given
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .email("duplicate@example.com")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, times(1)).existsByEmail("duplicate@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 활성화 성공")
    void testActivateUserSuccess() {
        // Given
        testUser.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.activateUser(1L);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 활성화 실패 - 이미 활성화된 사용자")
    void testActivateUserAlreadyActive() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.activateUser(1L))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 비활성화 성공")
    void testDeactivateUserSuccess() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deactivateUser(1L);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(refreshTokenRepository, times(1)).deleteByUser(testUser);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("관리자 승격 성공")
    void testPromoteToAdminSuccess() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.promoteToAdmin(1L);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("관리자 승격 실패 - 이미 관리자")
    void testPromoteToAdminAlreadyAdmin() {
        // Given
        testUser.setRole(User.Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.promoteToAdmin(1L))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("일반 사용자로 강등 성공")
    void testDemoteToUserSuccess() {
        // Given
        testUser.setRole(User.Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.demoteToUser(1L);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 삭제 성공")
    void testDeleteUserSuccess() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(refreshTokenRepository, times(1)).deleteByUser(testUser);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 존재하지 않는 사용자")
    void testDeleteUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).delete(any(User.class));
    }
}

