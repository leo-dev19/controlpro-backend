package com.controlpro.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DashboardMetricsResponse {
    private long totalEmployees;
    private long presentToday;
    private long lateToday;
    private long absentToday;
    private long pendingJustifications;
    private double attendanceRate;
    private List<DepartmentStat> departmentStats;
    private List<LiveFeedEntry> liveFeed;

    @Getter
    @Setter
    @Builder
    public static class DepartmentStat {
        private String area;
        private long punctual;
        private long late;
        private long absent;
    }

    @Getter
    @Setter
    @Builder
    public static class LiveFeedEntry {
        private String name;
        private String area;
        private String type; // Entrada, Salida
        private String time;
        private String status;
        private int minutesLate;
    }
}
