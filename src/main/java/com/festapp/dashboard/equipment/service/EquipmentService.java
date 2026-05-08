package com.festapp.dashboard.equipment.service;

import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.dashboard.widget.repository.DashboardWidgetRepository;
import com.festapp.dashboard.equipment.dto.EquipmentRequest;
import com.festapp.dashboard.equipment.dto.EquipmentResponse;
import com.festapp.dashboard.equipment.entity.Equipment;
import com.festapp.dashboard.equipment.repository.EquipmentRepository;
import com.festapp.dashboard.telemetry.entity.Sensor;
import com.festapp.dashboard.telemetry.repository.SensorNumericHistoryRepository;
import com.festapp.dashboard.telemetry.repository.SensorRepository;
import com.festapp.dashboard.telemetry.repository.SensorStringHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final DashboardRepository dashboardRepository;
    private final SensorRepository sensorRepository;
    private final SensorNumericHistoryRepository sensorNumericHistoryRepository;
    private final SensorStringHistoryRepository sensorStringHistoryRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;

    public EquipmentResponse createEquipment(Long userId, EquipmentRequest request) {
        Dashboard dashboard = getDashboardOrThrow(userId, request.getDashboardId());
        Equipment equipment = Equipment.builder()
                .dashboard(dashboard)
                .equipmentName(request.getEquipmentName())
                .field(request.getField())
                .build();
        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getDashboardEquipment(Long userId, Long dashboardId) {
        getDashboardOrThrow(userId, dashboardId);
        return equipmentRepository.findByDashboardDashboardIdOrderByEquipmentIdAsc(dashboardId)
                .stream()
                .map(EquipmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getEquipment(Long userId, Long equipmentId) {
        return EquipmentResponse.fromEntity(getEquipmentOrThrow(userId, equipmentId));
    }

    public EquipmentResponse updateEquipment(Long userId, Long equipmentId, EquipmentRequest request) {
        Equipment equipment = getEquipmentOrThrow(userId, equipmentId);
        Dashboard dashboard = getDashboardOrThrow(userId, request.getDashboardId());
        equipment.setDashboard(dashboard);
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setField(request.getField());
        return EquipmentResponse.fromEntity(equipmentRepository.save(equipment));
    }

    public void deleteEquipment(Long userId, Long equipmentId) {
        Equipment equipment = getEquipmentOrThrow(userId, equipmentId);
        dashboardWidgetRepository.clearEquipmentAndSensorReferences(equipmentId);

        List<Sensor> sensors = sensorRepository.findByEquipmentEquipmentIdOrderBySensorIdAsc(equipmentId);
        for (Sensor sensor : sensors) {
            sensorNumericHistoryRepository.deleteBySensorId(sensor.getSensorId());
            sensorStringHistoryRepository.deleteBySensorId(sensor.getSensorId());
        }
        sensorRepository.deleteAllInBatch(sensors);
        equipmentRepository.delete(equipment);
    }

    private Dashboard getDashboardOrThrow(Long userId, Long dashboardId) {
        return dashboardRepository.findByDashboardIdAndUserUserId(dashboardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND));
    }

    private Equipment getEquipmentOrThrow(Long userId, Long equipmentId) {
        return equipmentRepository.findByEquipmentIdAndDashboardUserUserId(equipmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EQUIPMENT_NOT_FOUND));
    }
}
