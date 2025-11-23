package com.crm.vo;

import lombok.Data;

/**
 * @author 小c
 */
@Data
public class DashboardResponse {
    private StatisticsData statistics;
    private TrendData trend;
}