package com.monitor.anomaly.model;

/**
 * 告警类型枚举
 */
public enum AlertType {
    
    /**
     * 单日暴涨
     */
    SINGLE_DAY_SPIKE("单日暴涨"),
    
    /**
     * 持续上涨
     */
    STEADY_RISE("持续上涨"),
    
    /**
     * 异常波动
     */
    ABNORMAL_VOLATILITY("异常波动"),
    
    /**
     * 单日暴跌
     */
    SINGLE_DAY_DROP("单日暴跌"),
    
    /**
     * 持续下降
     */
    STEADY_DECLINE("持续下降"),
    
    /**
     * 无异常
     */
    NO_ISSUE("无异常");
    
    private final String description;
    
    AlertType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
} 