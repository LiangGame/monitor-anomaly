package com.monitor.anomaly.model;

import com.monitor.anomaly.config.AnomalyDetectionConfig;
import com.monitor.anomaly.util.DataWindow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异常检测请求，包含数据窗口和自定义配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectionRequest {
    /**
     * 数据窗口
     */
    private DataWindow dataWindow;
    
    /**
     * 自定义配置，可选
     */
    private AnomalyDetectionConfig config;
} 