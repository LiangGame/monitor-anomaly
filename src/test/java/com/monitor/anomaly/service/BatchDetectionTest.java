package com.monitor.anomaly.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.monitor.anomaly.config.AnomalyDetectionConfig;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.AlertType;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.service.impl.AnomalyDetectionWindowServiceImpl;

/**
 * 批量数据检测测试类
 */
public class BatchDetectionTest {

    private AnomalyDetectionConfig config;
    private AnomalyDetectionWindowServiceImpl service;

    @BeforeEach
    void setUp() {
        // 初始化配置对象
        config = new AnomalyDetectionConfig();
        
        // 设置渐变上涨配置
        config.setGradualIncreaseSlopeThreshold(0.25);
        config.setGradualIncreaseMinRSquared(0.6);
        config.setGradualIncreaseMinConsecutiveIncreases(3);
        config.setGradualIncreaseTotalChangePercentThreshold(100.0);
        
        // 设置暴涨配置
        config.setSuddenSpikePercentageChangeThreshold(100.0);
        config.setSuddenSpikeStdDeviationMultiplier(3.0);
        config.setSuddenSpikeMinAbsoluteChange(10.0);
        
        // 创建服务实例并设置配置
        service = new AnomalyDetectionWindowServiceImpl();
        service.setConfig(config);
    }

    @Test
    void shouldDetectWithEmptyDataPoints() {
        // 创建空的数据点列表
        List<DataPointDTO> dataPoints = new ArrayList<>();
        
        // 执行检测
        AlertReport report = service.detectWithDataPoints(dataPoints);
        
        // 验证结果 - 应该返回正常状态
        assertFalse(report.isAlert());
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
        assertEquals("未提供数据点", report.getDescription());
    }
    
    @Test
    void shouldDetectGradualIncreaseWithDataPoints() {
        // 创建渐变上涨的数据点列表
        List<DataPointDTO> dataPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 添加渐变上涨的数据点
        dataPoints.add(new DataPointDTO(today.minusDays(6), 100.0));
        dataPoints.add(new DataPointDTO(today.minusDays(5), 130.0));  // +30%
        dataPoints.add(new DataPointDTO(today.minusDays(4), 170.0));  // +31%
        dataPoints.add(new DataPointDTO(today.minusDays(3), 225.0));  // +32%
        dataPoints.add(new DataPointDTO(today.minusDays(2), 300.0));  // +33%
        dataPoints.add(new DataPointDTO(today.minusDays(1), 400.0));  // +33%
        dataPoints.add(new DataPointDTO(today, 550.0));               // +38%, 总涨幅450%
        
        // 执行检测
        AlertReport report = service.detectWithDataPoints(dataPoints);
        
        // 验证结果 - 应该检测到渐变上涨
        assertTrue(report.isAlert(), "应当检测到告警状态，总涨幅450%");
        assertEquals(AlertType.STEADY_RISE, report.getAlertType(), "应当检测到稳定上涨类型");
    }
    
    @Test
    void shouldDetectSuddenSpikeWithDataPoints() {
        // 创建突然暴涨的数据点列表
        List<DataPointDTO> dataPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 添加正常波动后突然暴涨的数据点
        dataPoints.add(new DataPointDTO(today.minusDays(6), 100.0));
        dataPoints.add(new DataPointDTO(today.minusDays(5), 102.0));
        dataPoints.add(new DataPointDTO(today.minusDays(4), 99.0));
        dataPoints.add(new DataPointDTO(today.minusDays(3), 101.0));
        dataPoints.add(new DataPointDTO(today.minusDays(2), 100.0));
        dataPoints.add(new DataPointDTO(today.minusDays(1), 103.0));
        dataPoints.add(new DataPointDTO(today, 210.0));               // 暴涨约100%
        
        // 执行检测
        AlertReport report = service.detectWithDataPoints(dataPoints);
        
        // 验证结果 - 应该检测到单日暴涨
        assertTrue(report.isAlert(), "应当检测到告警状态");
        assertEquals(AlertType.SINGLE_DAY_SPIKE, report.getAlertType(), "应当检测到单日暴涨类型");
    }
    
    @Test
    void shouldHandleUnsortedDataPoints() {
        // 创建未排序的数据点列表
        List<DataPointDTO> dataPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 添加顺序打乱的数据点
        dataPoints.add(new DataPointDTO(today.minusDays(2), 300.0));  // 第5个
        dataPoints.add(new DataPointDTO(today.minusDays(5), 130.0));  // 第2个
        dataPoints.add(new DataPointDTO(today.minusDays(6), 100.0));  // 第1个
        dataPoints.add(new DataPointDTO(today.minusDays(3), 225.0));  // 第4个
        dataPoints.add(new DataPointDTO(today, 550.0));               // 第7个
        dataPoints.add(new DataPointDTO(today.minusDays(1), 400.0));  // 第6个
        dataPoints.add(new DataPointDTO(today.minusDays(4), 170.0));  // 第3个
        
        // 执行检测
        AlertReport report = service.detectWithDataPoints(dataPoints);
        
        // 验证结果 - 服务应该自动排序并检测到渐变上涨
        assertTrue(report.isAlert(), "应当检测到告警状态，总涨幅450%");
        assertEquals(AlertType.STEADY_RISE, report.getAlertType(), "应当检测到稳定上涨类型");
    }
    
    @Test
    void shouldDetectWithCustomConfig() {
        // 创建自定义配置对象
        AnomalyDetectionConfig customConfig = new AnomalyDetectionConfig();
        customConfig.setSuddenSpikePercentageChangeThreshold(30.0); // 设置较低的暴涨阈值
        
        // 创建数据点列表
        List<DataPointDTO> dataPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 添加小幅上涨的数据点
        dataPoints.add(new DataPointDTO(today.minusDays(6), 100.0));
        dataPoints.add(new DataPointDTO(today.minusDays(5), 102.0));
        dataPoints.add(new DataPointDTO(today.minusDays(4), 99.0));
        dataPoints.add(new DataPointDTO(today.minusDays(3), 101.0));
        dataPoints.add(new DataPointDTO(today.minusDays(2), 100.0));
        dataPoints.add(new DataPointDTO(today.minusDays(1), 103.0));
        dataPoints.add(new DataPointDTO(today, 140.0));              // 上涨36%，低于默认阈值但高于自定义阈值
        
        // 执行检测
        AlertReport report = service.detectWithDataPoints(dataPoints, customConfig);
        
        // 验证结果 - 应该基于自定义配置检测到单日暴涨
        assertTrue(report.isAlert(), "应当使用自定义配置并检测到告警");
        assertEquals(AlertType.SINGLE_DAY_SPIKE, report.getAlertType(), "应当检测到单日暴涨");
    }
} 