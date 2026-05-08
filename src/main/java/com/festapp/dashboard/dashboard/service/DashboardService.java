package com.festapp.dashboard.dashboard.service;

import com.festapp.dashboard.common.exception.ErrorCode;
import com.festapp.dashboard.common.exception.ResourceNotFoundException;
import com.festapp.dashboard.dashboard.dto.DashboardRequest;
import com.festapp.dashboard.dashboard.dto.DashboardResponse;
import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.user.entity.User;
import com.festapp.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    public DashboardResponse createDashboard(Long userId, DashboardRequest request) {
        User user = getUserOrThrow(userId);
        Dashboard dashboard = Dashboard.builder()
                .dashboardName(request.getDashboardName())
                .description(request.getDescription())
                .user(user)
                .build();
        return DashboardResponse.fromEntity(dashboardRepository.save(dashboard));
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> getMyDashboards(Long userId) {
        return dashboardRepository.findByUserUserIdOrderByDashboardIdAsc(userId)
                .stream()
                .map(DashboardResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, Long dashboardId) {
        return DashboardResponse.fromEntity(getDashboardOrThrow(userId, dashboardId));
    }

    public DashboardResponse updateDashboard(Long userId, Long dashboardId, DashboardRequest request) {
        Dashboard dashboard = getDashboardOrThrow(userId, dashboardId);
        dashboard.setDashboardName(request.getDashboardName());
        dashboard.setDescription(request.getDescription());
        return DashboardResponse.fromEntity(dashboardRepository.save(dashboard));
    }

    public void deleteDashboard(Long userId, Long dashboardId) {
        Dashboard dashboard = getDashboardOrThrow(userId, dashboardId);
        dashboardRepository.delete(dashboard);
    }

    private Dashboard getDashboardOrThrow(Long userId, Long dashboardId) {
        return dashboardRepository.findByDashboardIdAndUserUserId(dashboardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DASHBOARD_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
