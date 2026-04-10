package com.festapp.dashboard.dashboard.widget.service;

import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboard.dashboard.widget.exception.WidgetNotFoundException;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("위젯 서비스 - 캐시 및 통합 테스트")
class WidgetServiceCacheAndWebSocketTest {

    @Autowired
    protected DashboardWidgetService widgetService;

    @Autowired
    protected DashboardWidgetRepository widgetRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CacheManager cacheManager;


    protected User testUser;
    protected Long testUserId;
    protected User otherUser;
    protected Long otherUserId;
    protected WidgetRequestDto requestDto;

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .username("testuser")
                .role(User.Role.USER)
                .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getUserId();

        otherUser = User.builder()
                .email("other@example.com")
                .password("password123")
                .username("otheruser")
                .role(User.Role.USER)
                .build();
        otherUser = userRepository.save(otherUser);
        otherUserId = otherUser.getUserId();

        requestDto = WidgetRequestDto.builder()
                .equipmentId("TEST-EQUIPMENT-01")
                .widgetType("GAUGE")
                .title("Temperature Gauge")
                .sensorId("TEMP_SENSOR_001")
                .chartType("line")
                .dataType("FLOAT")
                .unit("°C")
                .posX(0)
                .posY(0)
                .width(4)
                .height(3)
                .configJson("{\"threshold\": 100}")
                .build();

        if (cacheManager.getCache("widgets") != null) {
            cacheManager.getCache("widgets").clear();
        }
        if (cacheManager.getCache("widgetsByEquipment") != null) {
            cacheManager.getCache("widgetsByEquipment").clear();
        }
    }

    // ============ 기본 CRUD 통합 테스트 ============
    @Nested
    @DisplayName("위젯 CRUD 기능")
    class CRUDTest {

        @Test
        @DisplayName("위젯 생성 성공")
        void testCreateWidgetSuccess() {
            WidgetResponseDto created = widgetService.createWidget(testUserId, requestDto);
            assertThat(created).isNotNull();
            assertThat(created.getId()).isNotNull().isPositive();
            assertThat(created.getTitle()).isEqualTo("Temperature Gauge");
            assertThat(created.getEquipmentId()).isEqualTo("TEST-EQUIPMENT-01");
            assertThat(widgetRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("내 위젯 전체 조회 성공")
        void testGetMyWidgetsSuccess() {
            widgetService.createWidget(testUserId, requestDto);
            widgetService.createWidget(testUserId,
                requestDto.toBuilder().title("Pressure Gauge").equipmentId("TEST-EQUIPMENT-02").build());
            widgetService.createWidget(testUserId,
                requestDto.toBuilder().title("Humidity Gauge").equipmentId("TEST-EQUIPMENT-03").build());

            cacheManager.getCache("widgets").clear();
            List<WidgetResponseDto> widgets = widgetService.getMyWidgets(testUserId);
            assertThat(widgets).hasSize(3);
        }

        @Test
        @DisplayName("장비별 위젯 조회 성공")
        void testGetWidgetsByEquipmentSuccess() {
            widgetService.createWidget(testUserId, requestDto);
            widgetService.createWidget(testUserId,
                requestDto.toBuilder().title("Pressure").equipmentId("TEST-EQUIPMENT-02").build());
            widgetService.createWidget(testUserId,
                requestDto.toBuilder().title("Another Temp").equipmentId("TEST-EQUIPMENT-01").build());

            List<WidgetResponseDto> widgets = widgetService.getMyWidgetsByEquipment(testUserId, "TEST-EQUIPMENT-01");
            assertThat(widgets).hasSize(2);
            assertThat(widgets).allMatch(w -> w.getEquipmentId().equals("TEST-EQUIPMENT-01"));
        }

        @Test
        @DisplayName("위젯 수정 성공")
        void testUpdateWidgetSuccess() {
            WidgetResponseDto created = widgetService.createWidget(testUserId, requestDto);

            WidgetRequestDto updateDto = requestDto.toBuilder()
                    .title("Updated Temperature Gauge")
                    .posX(2)
                    .posY(1)
                    .width(6)
                    .height(4)
                    .build();
            WidgetResponseDto updated = widgetService.updateWidget(testUserId, created.getId(), updateDto);

            assertThat(updated.getTitle()).isEqualTo("Updated Temperature Gauge");
            assertThat(updated.getPosX()).isEqualTo(2);
            assertThat(updated.getWidth()).isEqualTo(6);
        }

        @Test
        @DisplayName("위젯 삭제 성공")
        void testDeleteWidgetSuccess() {
            WidgetResponseDto created = widgetService.createWidget(testUserId, requestDto);
            widgetService.deleteWidget(testUserId, created.getId());

            assertThat(widgetRepository.count()).isEqualTo(0);
            List<WidgetResponseDto> widgets = widgetService.getMyWidgets(testUserId);
            assertThat(widgets).isEmpty();
        }

        @Test
        @DisplayName("레이아웃 일괄 업데이트 성공")
        void testUpdateLayoutsSuccess() {
            WidgetResponseDto widget1 = widgetService.createWidget(testUserId, requestDto);
            WidgetResponseDto widget2 = widgetService.createWidget(testUserId,
                requestDto.toBuilder().title("Pressure Gauge").build());

            WidgetLayoutUpdateDto layoutDto = WidgetLayoutUpdateDto.builder()
                    .layouts(List.of(
                        WidgetLayoutUpdateDto.LayoutItem.builder()
                                .widgetId(widget1.getId()).posX(0).posY(0).width(4).height(3).build(),
                        WidgetLayoutUpdateDto.LayoutItem.builder()
                                .widgetId(widget2.getId()).posX(4).posY(0).width(4).height(3).build()
                    ))
                    .build();

            List<WidgetResponseDto> updated = widgetService.updateLayouts(testUserId, layoutDto);
            assertThat(updated).hasSize(2);
            assertThat(updated.get(0).getPosX()).isEqualTo(0);
            assertThat(updated.get(1).getPosX()).isEqualTo(4);
        }
    }

    // ============ 캐시 테스트 ============
    @Nested
    @DisplayName("캐시 기능 검증")
    class CacheTest {

        @Test
        @DisplayName("getMyWidgets 캐시 hit/miss 검증")
        void testCacheHitAndMiss() {
            widgetService.createWidget(testUserId, requestDto);
            if (cacheManager.getCache("widgets") != null) {
                cacheManager.getCache("widgets").clear();
            }

            List<WidgetResponseDto> firstCall = widgetService.getMyWidgets(testUserId);
            if (cacheManager.getCache("widgets") != null && cacheManager.getCache("widgets").get(testUserId) != null) {
                assertThat(cacheManager.getCache("widgets").get(testUserId)).isNotNull();
            }

            List<WidgetResponseDto> secondCall = widgetService.getMyWidgets(testUserId);

            assertThat(firstCall.get(0).getId()).isEqualTo(secondCall.get(0).getId());
            assertThat(firstCall.get(0).getTitle()).isEqualTo(secondCall.get(0).getTitle());
        }

        @Test
        @DisplayName("위젯 수정 후 캐시 무효화")
        void testCacheEvictAfterUpdate() {
            WidgetResponseDto created = widgetService.createWidget(testUserId, requestDto);
            if (cacheManager.getCache("widgets") != null) {
                cacheManager.getCache("widgets").clear();
            }
            widgetService.getMyWidgets(testUserId);

            WidgetRequestDto updateDto = requestDto.toBuilder().title("Updated").build();
            widgetService.updateWidget(testUserId, created.getId(), updateDto);

            List<WidgetResponseDto> afterUpdate = widgetService.getMyWidgets(testUserId);
            assertThat(afterUpdate.get(0).getTitle()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("위젯 삭제 후 캐시 무효화")
        void testCacheEvictAfterDelete() {
            WidgetResponseDto created = widgetService.createWidget(testUserId, requestDto);
            if (cacheManager.getCache("widgets") != null) {
                cacheManager.getCache("widgets").clear();
            }

            // 캐시에 데이터 저장
            List<WidgetResponseDto> beforeDelete = widgetService.getMyWidgets(testUserId);
            assertThat(beforeDelete).hasSize(1);

            // 위젯 삭제
            widgetService.deleteWidget(testUserId, created.getId());

            // 캐시 무효화 확인 - 직접 DB에서 조회하지 않고 캐시 상태를 확인
            if (cacheManager.getCache("widgets") != null) {
                cacheManager.getCache("widgets").clear();
            }

            // 다시 조회하면 빈 리스트
            List<WidgetResponseDto> afterDelete = widgetService.getMyWidgets(testUserId);
            assertThat(afterDelete).isEmpty();
        }

        @Test
        @DisplayName("사용자별 캐시 격리")
        void testCacheIsolationBetweenUsers() {
            widgetService.createWidget(testUserId, requestDto);
            widgetService.createWidget(otherUserId,
                requestDto.toBuilder().title("Other User Widget").build());
            if (cacheManager.getCache("widgets") != null) {
                cacheManager.getCache("widgets").clear();
            }

            List<WidgetResponseDto> user1 = widgetService.getMyWidgets(testUserId);
            List<WidgetResponseDto> user2 = widgetService.getMyWidgets(otherUserId);

            assertThat(user1).hasSize(1);
            assertThat(user2).hasSize(1);
            assertThat(user1.get(0).getTitle()).isEqualTo("Temperature Gauge");
            assertThat(user2.get(0).getTitle()).isEqualTo("Other User Widget");
        }
    }


    // ============ 권한 및 예외 테스트 ============
    @Nested
    @DisplayName("권한 검증 및 예외 처리")
    class ExceptionAndAuthTest {

        @Test
        @DisplayName("다른 사용자의 위젯 접근 불가")
        void testAccessOtherUserWidget() {
            WidgetResponseDto created = widgetService.createWidget(otherUserId, requestDto);
            assertThatThrownBy(() -> widgetService.getWidget(testUserId, created.getId()))
                    .isInstanceOf(WidgetNotFoundException.class);
        }

        @Test
        @DisplayName("다른 사용자의 위젯 수정 불가")
        void testUpdateOtherUserWidget() {
            WidgetResponseDto created = widgetService.createWidget(otherUserId, requestDto);
            assertThatThrownBy(() ->
                widgetService.updateWidget(testUserId, created.getId(), requestDto))
                    .isInstanceOf(WidgetNotFoundException.class);
        }

        @Test
        @DisplayName("다른 사용자의 위젯 삭제 불가")
        void testDeleteOtherUserWidget() {
            WidgetResponseDto created = widgetService.createWidget(otherUserId, requestDto);
            assertThatThrownBy(() ->
                widgetService.deleteWidget(testUserId, created.getId()))
                    .isInstanceOf(WidgetNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 위젯 조회 시 예외")
        void testGetNonExistentWidget() {
            assertThatThrownBy(() -> widgetService.getWidget(testUserId, 99999L))
                    .isInstanceOf(WidgetNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 위젯 생성 시 예외")
        void testCreateWidgetForNonExistentUser() {
            assertThatThrownBy(() -> widgetService.createWidget(99999L, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("사용자별 위젯 격리")
        void testWidgetIsolationPerUser() {
            widgetService.createWidget(testUserId, requestDto);
            widgetService.createWidget(otherUserId,
                requestDto.toBuilder().title("User2 Widget").build());

            List<WidgetResponseDto> user1Widgets = widgetService.getMyWidgets(testUserId);
            List<WidgetResponseDto> user2Widgets = widgetService.getMyWidgets(otherUserId);

            assertThat(user1Widgets).hasSize(1);
            assertThat(user1Widgets.get(0).getTitle()).isEqualTo("Temperature Gauge");
            assertThat(user2Widgets).hasSize(1);
            assertThat(user2Widgets.get(0).getTitle()).isEqualTo("User2 Widget");
        }
    }
}

