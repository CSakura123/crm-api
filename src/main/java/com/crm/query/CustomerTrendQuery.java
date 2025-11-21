package com.crm.query;

import lombok.Data;
import org.apache.poi.ss.formula.IStabilityClassifier;

import java.util.List;

@Data
public class CustomerTrendQuery {
    private List<String> timeRange;
    private String transactionType;
    private String timeFormat;
}
