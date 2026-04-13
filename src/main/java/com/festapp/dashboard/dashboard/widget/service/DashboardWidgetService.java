// 대시보드 위젯 서비스 - 위젯 CRUD 및 레이아웃 관리 비즈니스 로직
package com.festapp.dashboard.dashboard.widget.service;

import com.festapp.dashboard.dashboard.widget.dto.WidgetLayoutUpdateDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboard.dashboard.widget.dto.WidgetWebSocketMessage;
import com.festapp.dashboard.dashboard.widget.entity.DashboardWidget;
import com.festapp.dashboard.dashboard.widget.exception.WidgetNotFoundException;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DashboardWidgetService {

    private final DashboardWidgetRepository widgetRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 공통 메서드: userId로 User 엔티티 조회
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with userId: {}", userId);
                    return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
                });
    }

    /**
     * 공통 메서드: widgetId와 userId로 Widget 조회
     */
    private DashboardWidget getWidgetOrThrow(Long widgetId, Long userId) {
        return widgetRepository.findByIdAndUserUserId(widgetId, userId)
                .orElseThrow(() -> {
                    log.warn("Widget not found with id: {} for user: {}", widgetId, userId);
                    return new WidgetNotFoundException(ErrorCode.WIDGET_NOT_FOUND);
                });
    }

    @CacheEvict(value = {"widgets", "widgetsByEquipment"}, allEntries = true)
    public WidgetResponseDto createWidget(Long userId, WidgetRequestDto dto) {
        log.debug("Creating widget for user: {}", userId);
        
        User user = getUserOrThrow(userId);

        DashboardWidget widget = DashboardWidget.builder()
                .user(user)
                .equipmentId(dto.getEquipmentId())
                .widgetType(dto.getWidgetType())
                .title(dto.getTitle())
                .sensorId(dto.getSensorId())
                .chartType(dto.getChartType())
                .dataType(dto.getDataType())
                .unit(dto.getUnit())
                .posX(dto.getPosX())
                .posY(dto.getPosY())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .configJson(dto.getConfigJson())
                .build();

        DashboardWidget savedWidget = widgetRepository.save(widget);
        WidgetResponseDto response = WidgetResponseDto.fromEntity(savedWidget);

        log.info("Widget created successfully with id: {} for user: {}", savedWidget.getId(), userId);

        // WebSocket 메시지 브로드캐스트
        broadcastWidgetUpdate(WidgetWebSocketMessage.MessageType.WIDGET_CREATED, response, userId);

        return response;
    }

    @Cacheable(value = "widgets", key = "#userId")
    public List<WidgetResponseDto> getMyWidgets(Long userId) {
        log.debug("Cache MISS - Fetching all widgets for user: {}", userId);
        return widgetRepository.findByUserUserIdOrderByIdAsc(userId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();
    }

    @Cacheable(value = "widgetsByEquipment", key = "#userId + ':' + #equipmentId")
    public List<WidgetResponseDto> getMyWidgetsByEquipment(Long userId, String equipmentId) {
        log.debug("Cache MISS - Fetching widgets for user: {} with equipmentId: {}", userId, equipmentId);
        return widgetRepository.findByUserUserIdAndEquipmentIdOrderByIdAsc(userId, equipmentId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();
    }

    public WidgetResponseDto getWidget(Long userId, Long widgetId) {
        log.debug("Fetching widget with id: {} for user: {}", widgetId, userId);
        DashboardWidget widget = getWidgetOrThrow(widgetId, userId);
        return WidgetResponseDto.fromEntity(widget);
    }

    @CacheEvict(value = {"widgets", "widgetsByEquipment"}, allEntries = true)
    public WidgetResponseDto updateWidget(Long userId, Long widgetId, WidgetRequestDto dto) {
        log.info("Updating widget with id: {} for user: {}", widgetId, userId);
        DashboardWidget widget = getWidgetOrThrow(widgetId, userId);

        String oldEquipmentId = widget.getEquipmentId();
        String newEquipmentId = dto.getEquipmentId();

        // equipmentId 변경 시 이전 장비 캐시도 무효화
        boolean equipmentChanged = !oldEquipmentId.equals(newEquipmentId);
        if (equipmentChanged) {
            log.debug("Equipment changed from {} to {} - clearing both caches", oldEquipmentId, newEquipmentId);
        }

        widget.setEquipmentId(newEquipmentId);
        widget.setWidgetType(dto.getWidgetType());
        widget.setTitle(dto.getTitle());
        widget.setSensorId(dto.getSensorId());
        widget.setChartType(dto.getChartType());
        widget.setDataType(dto.getDataType());
        widget.setUnit(dto.getUnit());
        widget.setPosX(dto.getPosX());
        widget.setPosY(dto.getPosY());
        widget.setWidth(dto.getWidth());
        widget.setHeight(dto.getHeight());
        widget.setConfigJson(dto.getConfigJson());

        DashboardWidget updatedWidget = widgetRepository.save(widget);
        WidgetResponseDto response = WidgetResponseDto.fromEntity(updatedWidget);

        log.info("Widget updated successfully with id: {} for user: {}", widgetId, userId);

        // WebSocket 메시지 브로드캐스트
        broadcastWidgetUpdate(WidgetWebSocketMessage.MessageType.WIDGET_UPDATED, response, userId);

        return response;
    }


    @CacheEvict(value = {"widgets", "widgetsByEquipment"}, allEntries = true)
    public void deleteWidget(Long userId, Long widgetId) {
        log.info("Deleting widget with id: {} for user: {}", widgetId, userId);
        DashboardWidget widget = getWidgetOrThrow(widgetId, userId);

        String equipmentId = widget.getEquipmentId();
        Long deletedWidgetId = widget.getId();

        widgetRepository.delete(widget);
        log.info("Widget deleted successfully with id: {} for user: {}", widgetId, userId);

        // WebSocket 메시지 브로드캐스트
        WidgetWebSocketMessage wsMessage = WidgetWebSocketMessage.builder()
                .messageType(WidgetWebSocketMessage.MessageType.WIDGET_DELETED)
                .widgetId(deletedWidgetId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(
            "/topic/user/" + userId + "/widgets",
            wsMessage
        );
        messagingTemplate.convertAndSend(
            "/topic/equipment/" + equipmentId + "/widgets",
            wsMessage
        );
    }

    @CacheEvict(value = {"widgets", "widgetsByEquipment"}, allEntries = true)
    public List<WidgetResponseDto> updateLayouts(Long userId, WidgetLayoutUpdateDto dto) {
        log.info("Updating layouts for user: {}", userId);

        if (dto.getLayouts() == null || dto.getLayouts().isEmpty()) {
            log.warn("No layouts provided for user: {}", userId);
            return widgetRepository.findByUserUserIdOrderByIdAsc(userId)
                    .stream()
                    .map(WidgetResponseDto::fromEntity)
                    .toList();
        }

        List<WidgetResponseDto> updatedWidgets = new ArrayList<>();

        for (WidgetLayoutUpdateDto.LayoutItem layout : dto.getLayouts()) {
            DashboardWidget widget = getWidgetOrThrow(layout.getWidgetId(), userId);

            widget.setPosX(layout.getPosX());
            widget.setPosY(layout.getPosY());
            widget.setWidth(layout.getWidth());
            widget.setHeight(layout.getHeight());

            DashboardWidget saved = widgetRepository.save(widget);
            WidgetResponseDto response = WidgetResponseDto.fromEntity(saved);
            updatedWidgets.add(response);

            log.debug("Layout updated for widget id: {}", layout.getWidgetId());

            // 각 위젯별 WebSocket 메시지 브로드캐스트
            broadcastWidgetUpdate(WidgetWebSocketMessage.MessageType.LAYOUT_UPDATED, response, userId);
        }

        log.info("All layouts updated successfully for user: {}", userId);
        return updatedWidgets;
    }

    /**
     * 위젯 변경 이벤트를 WebSocket으로 브로드캐스트
     */
    private void broadcastWidgetUpdate(String messageType, WidgetResponseDto widget, Long userId) {
        WidgetWebSocketMessage wsMessage = WidgetWebSocketMessage.builder()
                .messageType(messageType)
                .widgetId(widget.getId())
                .userId(userId)
                .widget(widget)
                .timestamp(LocalDateTime.now())
                .build();

        // 사용자별 구독 토픽에 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/user/" + userId + "/widgets",
            wsMessage
        );

        // 장비별 구독 토픽에도 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/equipment/" + widget.getEquipmentId() + "/widgets",
            wsMessage
        );
    }
}
