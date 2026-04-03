// 대시보드 위젯 서비스 - 위젯 CRUD 및 레이아웃 관리 비즈니스 로직
package com.festapp.dashboar.dashboard.widget.service;

import com.festapp.dashboar.dashboard.widget.dto.WidgetLayoutUpdateDto;
import com.festapp.dashboar.dashboard.widget.dto.WidgetRequestDto;
import com.festapp.dashboar.dashboard.widget.dto.WidgetResponseDto;
import com.festapp.dashboar.dashboard.widget.entity.DashboardWidget;
import com.festapp.dashboar.dashboard.widget.exception.WidgetNotFoundException;
import com.festapp.dashboar.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboar.user.entity.User;
import com.festapp.dashboar.user.repository.UserRepository;
import com.festapp.dashboar.common.exception.ResourceNotFoundException;
import com.festapp.dashboar.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DashboardWidgetService {

    private final DashboardWidgetRepository widgetRepository;
    private final UserRepository userRepository;

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
        log.info("Widget created successfully with id: {} for user: {}", savedWidget.getId(), userId);
        return WidgetResponseDto.fromEntity(savedWidget);
    }

    public List<WidgetResponseDto> getMyWidgets(Long userId) {
        log.debug("Fetching all widgets for user: {}", userId);
        return widgetRepository.findByUserUserIdOrderByIdAsc(userId)
                .stream()
                .map(WidgetResponseDto::fromEntity)
                .toList();
    }

    public List<WidgetResponseDto> getMyWidgetsByEquipment(Long userId, String equipmentId) {
        log.debug("Fetching widgets for user: {} with equipmentId: {}", userId, equipmentId);
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

    public WidgetResponseDto updateWidget(Long userId, Long widgetId, WidgetRequestDto dto) {
        log.info("Updating widget with id: {} for user: {}", widgetId, userId);
        DashboardWidget widget = getWidgetOrThrow(widgetId, userId);

        widget.setEquipmentId(dto.getEquipmentId());
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
        log.info("Widget updated successfully with id: {} for user: {}", widgetId, userId);
        return WidgetResponseDto.fromEntity(updatedWidget);
    }

    public void deleteWidget(Long userId, Long widgetId) {
        log.info("Deleting widget with id: {} for user: {}", widgetId, userId);
        DashboardWidget widget = getWidgetOrThrow(widgetId, userId);
        widgetRepository.delete(widget);
        log.info("Widget deleted successfully with id: {} for user: {}", widgetId, userId);
    }

    public List<WidgetResponseDto> updateLayouts(Long userId, WidgetLayoutUpdateDto dto) {
        log.info("Updating layouts for user: {}", userId);

        if (dto.getLayouts() == null || dto.getLayouts().isEmpty()) {
            log.warn("No layouts provided for user: {}", userId);
            return getMyWidgets(userId);
        }

        for (WidgetLayoutUpdateDto.LayoutItem layout : dto.getLayouts()) {
            DashboardWidget widget = getWidgetOrThrow(layout.getWidgetId(), userId);

            widget.setPosX(layout.getPosX());
            widget.setPosY(layout.getPosY());
            widget.setWidth(layout.getWidth());
            widget.setHeight(layout.getHeight());

            widgetRepository.save(widget);
            log.debug("Layout updated for widget id: {}", layout.getWidgetId());
        }

        log.info("All layouts updated successfully for user: {}", userId);
        return getMyWidgets(userId);
    }
}
