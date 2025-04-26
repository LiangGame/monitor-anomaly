package com.monitor.anomaly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 告警报告模型类，用于存储告警检测结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertReport {
    
    /**
     * 告警日期
     */
    private LocalDate date;
    
    /**
     * 总分数 - 反映告警的严重程度
     */
    private double totalScore;
    
    /**
     * 告警类型
     */
    private AlertType alertType;
    
    /**
     * 告警描述
     */
    private String description;
    
    /**
     * 是否为告警
     */
    @Builder.Default
    private boolean isAlert = false;
    
    /**
     * 异常严重程度
     */
    private SeverityLevel severityLevel;
    
    /**
     * 异常严重程度枚举
     */
    public enum SeverityLevel {
        NORMAL,     // 正常
        WARNING,    // 警告
        CRITICAL    // 严重
    }
} 