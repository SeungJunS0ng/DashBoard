// 대시보드 위젯 엔티티 - 위젯 정보를 저장하는 데이터베이스 테이블
package com.festapp.dashboar.dashboard.widget.entity;

import com.festapp.dashboar.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_widgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class DashboardWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "equipment_id", nullable = false, length = 255)
    private String equipmentId;

    @Column(name = "widget_type", nullable = false, length = 50)
    private String widgetType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "sensor_id", length = 255)
    private String sensorId;

    @Column(name = "chart_type", length = 50)
    private String chartType;

    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "pos_x", nullable = false)
    @Builder.Default
    private Integer posX = 0;

    @Column(name = "pos_y", nullable = false)
    @Builder.Default
    private Integer posY = 0;

    @Column(name = "width", nullable = false)
    @Builder.Default
    private Integer width = 1;

    @Column(name = "height", nullable = false)
    @Builder.Default
    private Integer height = 1;

    @Column(name = "config_json", columnDefinition = "LONGTEXT")
    private String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

