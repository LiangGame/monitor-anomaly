package com.monitor.anomaly.service;

import java.util.Date;
import java.util.List;

import com.monitor.anomaly.config.DeclineDetectionConfig;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.util.DataWindow;

/**
 * 下跌检测服务接口
 * 提供各种检测数据下跌的方法
 */
public interface DeclineDetectionService {

    /**
     * 使用全局默认配置检测数据窗口中的下跌
     * @param dataWindow 数据窗口
     * @return 告警报告
     */
    AlertReport detectDecline(DataWindow dataWindow);
    
    /**
     * 使用自定义配置检测数据窗口中的下跌
     * @param dataWindow 数据窗口
     * @param customConfig 自定义配置
     * @return 告警报告
     */
    AlertReport detectDecline(DataWindow dataWindow, DeclineDetectionConfig customConfig);
    
    /**
     * 使用全局默认配置检测数值列表中的下跌
     * @param values 数值列表
     * @return 告警报告
     */
    AlertReport detectWithValues(List<Double> values);
    
    /**
     * 使用自定义配置检测数值列表中的下跌
     * @param values 数值列表
     * @param customConfig 自定义配置
     * @return 告警报告
     */
    AlertReport detectWithValues(List<Double> values, DeclineDetectionConfig customConfig);
    
    /**
     * 使用数值列表检测下跌，使用全局配置
     * 
     * @param values 数值列表
     * @return 告警报告
     */
    AlertReport detectDeclineWithValues(List<Double> values);
    
    /**
     * 使用数值列表检测下跌，使用自定义配置
     * 
     * @param values        数值列表
     * @param customConfig  自定义配置
     * @return 告警报告
     */
    AlertReport detectDeclineWithValues(List<Double> values, DeclineDetectionConfig customConfig);
    
    /**
     * 添加数据点并检测下跌
     * 
     * @param dataWindow 数据窗口
     * @param date 日期
     * @param value 值
     * @return 告警报告，包含检测结果
     */
    AlertReport addPointAndDetect(DataWindow dataWindow, Date date, double value);
    
    /**
     * 添加数据点并使用自定义配置检测下跌
     * 
     * @param dataWindow 数据窗口
     * @param date 日期
     * @param value 值
     * @param customConfig 自定义配置
     * @return 告警报告，包含检测结果
     */
    AlertReport addPointAndDetect(DataWindow dataWindow, Date date, double value, DeclineDetectionConfig customConfig);
    
    /**
     * 使用数据点列表检测下跌
     * 
     * @param dataPoints 数据点列表
     * @return 告警报告，包含检测结果
     */
    AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints);
    
    /**
     * 使用数据点列表和自定义配置检测下跌
     * 
     * @param dataPoints 数据点列表
     * @param customConfig 自定义配置
     * @return 告警报告，包含检测结果
     */
    AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints, DeclineDetectionConfig customConfig);
} 