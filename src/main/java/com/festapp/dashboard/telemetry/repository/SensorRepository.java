package com.festapp.dashboard.telemetry.repository;

import com.festapp.dashboard.telemetry.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Optional<Sensor> findBySensorIdAndEquipmentEquipmentId(Long sensorId, Long equipmentId);

    Optional<Sensor> findBySensorIdAndEquipmentDashboardUserUserId(Long sensorId, Long userId);

    Optional<Sensor> findBySensorNameAndEquipmentEquipmentId(String sensorName, Long equipmentId);

    List<Sensor> findByEquipmentEquipmentIdOrderBySensorIdAsc(Long equipmentId);
}
