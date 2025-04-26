package com.monitor.anomaly.controller;

import com.monitor.anomaly.config.AnomalyDetectionConfig;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.BatchDetectionRequest;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.model.DetectionRequest;
import com.monitor.anomaly.service.AnomalyDetectionWindowService;
import com.monitor.anomaly.util.DataWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/anomaly/window")
@RequiredArgsConstructor
public class AnomalyDetectionWindowController {

    private final AnomalyDetectionWindowService anomalyDetectionWindowService;

    /**
     * 使用默认配置检测异常
     * @param date 日期
     * @param value 指标值
     * @return 告警报告
     */
    @GetMapping("/detect")
    public ResponseEntity<AlertReport> detectAnomaly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam double value) {
        AlertReport report = anomalyDetectionWindowService.addPointAndDetect(date, value);
        return ResponseEntity.ok(report);
    }
    
    /**
     * 批量数据分析
     * @param dataWindow 数据窗口
     * @return 告警报告
     */
    @PostMapping("/batch")
    public ResponseEntity<AlertReport> detectAnomalyWithWindow(@RequestBody DataWindow dataWindow) {
        AlertReport report = anomalyDetectionWindowService.detectAnomaly(dataWindow);
        return ResponseEntity.ok(report);
    }
    
    /**
     * 批量数据点检测 - 提供数据点列表而不是完整的数据窗口
     * @param dataPoints 数据点列表
     * @return 告警报告
     */
    @PostMapping("/batch-points")
    public ResponseEntity<AlertReport> detectWithDataPoints(@RequestBody List<DataPointDTO> dataPoints) {
        AlertReport report = anomalyDetectionWindowService.detectWithDataPoints(dataPoints);
        return ResponseEntity.ok(report);
    }
    
    /**
     * 批量数据点检测（带自定义配置）
     * @param request 包含数据点列表和配置的请求对象
     * @return 告警报告
     */
    @PostMapping("/batch-points-with-config")
    public ResponseEntity<AlertReport> detectWithDataPointsAndConfig(@RequestBody BatchDetectionRequest<DataPointDTO, AnomalyDetectionConfig> request) {
        AlertReport report = anomalyDetectionWindowService.detectWithDataPoints(
                request.getDataPoints(), 
                request.getConfig());
        return ResponseEntity.ok(report);
    }
    
    /**
     * 纯值列表检测 - 提供值列表而不是完整的数据点
     * @param values 值列表，假设按时间顺序排列（从最早到最近）
     * @return 告警报告
     */
    @PostMapping("/values")
    public ResponseEntity<AlertReport> detectWithValues(@RequestBody List<Double> values) {
        AlertReport report = anomalyDetectionWindowService.detectWithValues(values);
        return ResponseEntity.ok(report);
    }
    
    /**
     * 纯值列表检测（带自定义配置）
     * @param request 包含值列表和配置的请求对象
     * @return 告警报告
     */
    @PostMapping("/values-with-config")
    public ResponseEntity<AlertReport> detectWithValuesAndConfig(@RequestBody BatchDetectionRequest<Double, AnomalyDetectionConfig> request) {
        AlertReport report = anomalyDetectionWindowService.detectWithValues(
                request.getDataPoints(), 
                request.getConfig());
        return ResponseEntity.ok(report);
    }
    
    /**
     * 使用自定义配置检测异常
     * @param request 检测请求，包含数据窗口和自定义配置
     * @return 告警报告
     */
    @PostMapping("/detect-with-config")
    public ResponseEntity<AlertReport> detectAnomalyWithConfig(@RequestBody DetectionRequest request) {
        AlertReport report = anomalyDetectionWindowService.detectAnomaly(
                request.getDataWindow(), 
                request.getConfig());
        return ResponseEntity.ok(report);
    }
    
    /**
     * 添加单点并使用自定义配置检测异常
     * @param date 日期
     * @param value 指标值
     * @param customConfig 自定义配置
     * @return 告警报告
     */
    @PostMapping("/point-with-config")
    public ResponseEntity<AlertReport> detectPointWithConfig(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam double value,
            @RequestBody AnomalyDetectionConfig customConfig) {
        
        AlertReport report = anomalyDetectionWindowService.addPointAndDetect(
                date, 
                value, 
                customConfig);
        return ResponseEntity.ok(report);
    }
} 