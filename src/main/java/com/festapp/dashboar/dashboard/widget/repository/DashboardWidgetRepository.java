// 대시보드 위젯 리포지토리 - 위젯 데이터 접근 계층
package com.festapp.dashboar.dashboard.widget.repository;

import com.festapp.dashboar.dashboard.widget.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    List<DashboardWidget> findByUserUserIdOrderByIdAsc(Long userId);

    List<DashboardWidget> findByUserUserIdAndEquipmentIdOrderByIdAsc(Long userId, String equipmentId);

    Optional<DashboardWidget> findByIdAndUserUserId(Long id, Long userId);

    @Query("SELECT COUNT(w) FROM DashboardWidget w WHERE w.user.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("DELETE FROM DashboardWidget w WHERE w.user.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM DashboardWidget w WHERE w.user.userId = :userId AND w.equipmentId = :equipmentId")
    long countByUserIdAndEquipmentId(@Param("userId") Long userId, @Param("equipmentId") String equipmentId);
}

