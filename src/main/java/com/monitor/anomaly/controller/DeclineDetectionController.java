package com.monitor.anomaly.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.monitor.anomaly.config.DeclineDetectionConfig;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.BatchDetectionRequest;
import com.monitor.anomaly.service.DeclineDetectionService;

import lombok.extern.slf4j.Slf4j;

/**
 * 下跌检测控制器
 * 提供下跌检测的REST API
 */
@RestController
@RequestMapping("/api/decline")
@Slf4j
public class DeclineDetectionController {

    @Autowired
    private DeclineDetectionService declineDetectionService;
    
    /**
     * 使用值列表检测下跌
     * POST /api/decline/batch-values
     * 
     * @param values 值列表
     * @return 告警报告
     */
    @PostMapping("/batch-values")
    public AlertReport detectWithValues(@RequestBody List<Double> values) {
        log.info("接收到批量值检测请求，共 {} 个值", values.size());
        return declineDetectionService.detectWithValues(values);
    }
    
    /**
     * 使用值列表和自定义配置检测下跌
     * POST /api/decline/batch-values-with-config
     * 
     * @param request 包含值列表和自定义配置的请求
     * @return 告警报告
     */
    @PostMapping("/batch-values-with-config")
    public AlertReport detectWithValuesAndConfig(@RequestBody BatchDetectionRequest<Double, DeclineDetectionConfig> request) {
        log.info("接收到带自定义配置的批量值检测请求，共 {} 个值", request.getDataPoints().size());
        return declineDetectionService.detectWithValues(request.getDataPoints(), request.getConfig());
    }
    
    /**
     * 使用数据点列表检测下跌
     * POST /api/decline/batch-datapoints
     * 
     * @param dataPoints 数据点列表
     * @return 告警报告
     */
    @PostMapping("/batch-datapoints")
    public AlertReport detectWithDataPoints(@RequestBody List<DataPointDTO> dataPoints) {
        log.info("接收到批量数据点检测请求，共 {} 个数据点", dataPoints.size());
        return declineDetectionService.detectWithDataPoints(dataPoints);
    }
    
    /**
     * 使用数据点列表和自定义配置检测下跌
     * POST /api/decline/batch-datapoints-with-config
     * 
     * @param request 包含数据点列表和自定义配置的请求
     * @return 告警报告
     */
    @PostMapping("/batch-datapoints-with-config")
    public AlertReport detectWithDataPointsAndConfig(@RequestBody BatchDetectionRequest<DataPointDTO, DeclineDetectionConfig> request) {
        log.info("接收到带自定义配置的批量数据点检测请求，共 {} 个数据点", request.getDataPoints().size());
        return declineDetectionService.detectWithDataPoints(request.getDataPoints(), request.getConfig());
    }
} 