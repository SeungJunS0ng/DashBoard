package com.festapp.dashboard.dashboard.service;

import com.festapp.dashboard.dashboard.entity.Dashboard;
import com.festapp.dashboard.dashboard.repository.DashboardRepository;
import com.festapp.dashboard.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardProvisioningService {

    private final DashboardRepository dashboardRepository;

    @Transactional
    public Dashboard ensureDefaultDashboard(User user) {
        return dashboardRepository.findFirstByUserUserIdOrderByDashboardIdAsc(user.getUserId())
                .orElseGet(() -> dashboardRepository.save(
                        Dashboard.builder()
                                .dashboardName(user.getUsername() + " Dashboard")
                                .description("Default dashboard for " + user.getUsername())
                                .user(user)
                                .build()
                ));
    }
}
