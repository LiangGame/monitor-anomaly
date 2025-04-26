package com.monitor.anomaly.service;

import com.monitor.anomaly.config.AnomalyDetectionConfig;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.util.DataWindow;

import java.time.LocalDate;
import java.util.List;

/**
 * 基于窗口的异常检测服务接口
 */
public interface AnomalyDetectionWindowService {
    
    /**
     * 分析窗口数据，检测异常（使用全局配置）
     * 
     * @param dataWindow 数据窗口
     * @return 告警报告
     */
    AlertReport detectAnomaly(DataWindow dataWindow);
    
    /**
     * 分析窗口数据，检测异常（支持自定义配置）
     * 
     * @param dataWindow 数据窗口
     * @param customConfig 自定义配置（如为null则使用全局配置）
     * @return 告警报告
     */
    AlertReport detectAnomaly(DataWindow dataWindow, AnomalyDetectionConfig customConfig);
    
    /**
     * 实时添加数据点并检测异常（使用全局配置）
     * 
     * @param date 日期
     * @param value 值
     * @return 告警报告
     */
    AlertReport addPointAndDetect(java.time.LocalDate date, double value);
    
    /**
     * 实时添加数据点并检测异常（支持自定义配置）
     * 
     * @param date 日期
     * @param value 值
     * @param customConfig 自定义配置（如为null则使用全局配置）
     * @return 告警报告
     */
    AlertReport addPointAndDetect(java.time.LocalDate date, double value, AnomalyDetectionConfig customConfig);
    
    /**
     * 批量处理数据点并检测异常（使用全局配置）
     * 
     * @param dataPoints 数据点列表
     * @return 告警报告
     */
    AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints);
    
    /**
     * 批量处理数据点并检测异常（支持自定义配置）
     * 
     * @param dataPoints 数据点列表
     * @param customConfig 自定义配置（如为null则使用全局配置）
     * @return 告警报告
     */
    AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints, AnomalyDetectionConfig customConfig);
    
    /**
     * 使用纯值列表检测异常（使用全局配置）
     * 假设数据是按天顺序排列的，从今天往前推
     * 
     * @param values 值列表
     * @return 告警报告
     */
    AlertReport detectWithValues(List<Double> values);
    
    /**
     * 使用纯值列表检测异常（支持自定义配置）
     * 假设数据是按天顺序排列的，从今天往前推
     * 
     * @param values 值列表
     * @param customConfig 自定义配置（如为null则使用全局配置）
     * @return 告警报告
     */
    AlertReport detectWithValues(List<Double> values, AnomalyDetectionConfig customConfig);
} 