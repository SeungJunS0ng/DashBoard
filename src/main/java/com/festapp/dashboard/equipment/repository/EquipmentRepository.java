package com.festapp.dashboard.equipment.repository;

import com.festapp.dashboard.equipment.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByEquipmentIdAndDashboardDashboardId(Long equipmentId, Long dashboardId);

    Optional<Equipment> findByEquipmentIdAndDashboardUserUserId(Long equipmentId, Long userId);

    Optional<Equipment> findByEquipmentNameAndDashboardDashboardId(String equipmentName, Long dashboardId);

    Optional<Equipment> findFirstByEquipmentName(String equipmentName);

    List<Equipment> findByDashboardDashboardIdOrderByEquipmentIdAsc(Long dashboardId);
}
