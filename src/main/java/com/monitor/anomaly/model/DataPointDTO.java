package com.monitor.anomaly.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 数据点传输对象，用于批量API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPointDTO {
    
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 值
     */
    private double value;
} 