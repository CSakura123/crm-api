package com.crm.common.dto;

import lombok.Data;

@Data
public class DashboardTrendDTO {
    private DashboardStatisticsDTO statistics;
    private DashboardTrendDTO trend;
}
