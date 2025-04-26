package com.monitor.anomaly.model;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量检测请求类
 * 包含数据点和可选的自定义配置
 * @param <T> 数据点类型，可以是Double或DataPointDTO
 * @param <C> 配置类型，可以是AnomalyDetectionConfig或DeclineDetectionConfig
 */
@Data
@NoArgsConstructor
public class BatchDetectionRequest<T, C> {
    
    /**
     * 数据点列表
     */
    private List<T> dataPoints;
    
    /**
     * 自定义配置，可选
     * 可以是AnomalyDetectionConfig（上涨检测）或DeclineDetectionConfig（下跌检测）
     */
    private C config;
} 