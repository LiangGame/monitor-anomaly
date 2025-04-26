package com.monitor.anomaly.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.monitor.anomaly.config.AnomalyDetectionConfig;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.AlertType;
import com.monitor.anomaly.service.impl.AnomalyDetectionWindowServiceImpl;

/**
 * 纯值列表检测测试类
 */
public class ValuesDetectionTest {

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
        
        // 使用建造者模式创建服务实例并设置配置
        service = AnomalyDetectionWindowServiceImpl.builder()
                .withConfig(config)
                .withWindowSize(7)
                .build();
    }

    @Test
    void shouldDetectWithEmptyValues() {
        // 创建空的值列表
        List<Double> values = new ArrayList<>();
        
        // 执行检测
        AlertReport report = service.detectWithValues(values);
        
        // 验证结果 - 应该返回正常状态
        assertFalse(report.isAlert());
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
        assertEquals("未提供数据点", report.getDescription());
    }
    
    @Test
    void shouldDetectGradualIncreaseWithValues() {
        // 创建渐变上涨的值列表（从最早到最近）
        List<Double> values = Arrays.asList(
                100.0,   // 最早
                130.0,   // +30%
                170.0,   // +31%
                225.0,   // +32%
                300.0,   // +33%
                400.0,   // +33%
                550.0    // +38%, 总涨幅450%
        );
        
        // 执行检测
        AlertReport report = service.detectWithValues(values);
        
        // 验证结果 - 应该检测到渐变上涨
        assertTrue(report.isAlert(), "应当检测到告警状态，总涨幅450%");
        assertEquals(AlertType.STEADY_RISE, report.getAlertType(), "应当检测到稳定上涨类型");
    }
    
    @Test
    void shouldDetectSuddenSpikeWithValues() {
        // 创建突然暴涨的值列表（从最早到最近）
        List<Double> values = Arrays.asList(
                100.0,
                102.0,
                99.0,
                101.0,
                100.0,
                103.0,
                210.0    // 暴涨约100%
        );
        
        // 执行检测
        AlertReport report = service.detectWithValues(values);
        
        // 验证结果 - 应该检测到单日暴涨
        assertTrue(report.isAlert(), "应当检测到告警状态");
        assertEquals(AlertType.SINGLE_DAY_SPIKE, report.getAlertType(), "应当检测到单日暴涨类型");
    }
    
    @Test
    void shouldDetectWithCustomConfig() {
        // 创建自定义配置对象
        AnomalyDetectionConfig customConfig = new AnomalyDetectionConfig();
        customConfig.setSuddenSpikePercentageChangeThreshold(30.0); // 设置较低的暴涨阈值
        
        // 创建小幅上涨的值列表（从最早到最近）
        List<Double> values = Arrays.asList(
                100.0,
                102.0,
                99.0,
                101.0,
                100.0,
                103.0,
                140.0   // 上涨36%，低于默认阈值但高于自定义阈值
        );
        
        // 执行检测
        AlertReport report = service.detectWithValues(values, customConfig);
        
        // 验证结果 - 应该基于自定义配置检测到单日暴涨
        assertTrue(report.isAlert(), "应当使用自定义配置并检测到告警");
        assertEquals(AlertType.SINGLE_DAY_SPIKE, report.getAlertType(), "应当检测到单日暴涨");
    }
    
    @Test
    void shouldHandleSmallValueList() {
        // 创建小于默认窗口大小的值列表
        List<Double> values = Arrays.asList(
                100.0,
                101.0,
                150.0   // 小幅上涨后突然涨幅50%
        );
        
        // 执行检测
        AlertReport report = service.detectWithValues(values);
        
        // 验证结果 - 因为数据少于3个点就能检测到结果
        assertNotNull(report);
        if (report.isAlert()) {
            assertEquals(AlertType.SINGLE_DAY_SPIKE, report.getAlertType(), "应当检测到单日暴涨");
        }
    }
    
    @Test
    void shouldCreateServiceWithFactoryMethods() {
        // 测试静态工厂方法
        AnomalyDetectionWindowServiceImpl defaultService = AnomalyDetectionWindowServiceImpl.createDefault();
        AnomalyDetectionWindowServiceImpl configuredService = AnomalyDetectionWindowServiceImpl.create(config);
        AnomalyDetectionWindowServiceImpl fullConfiguredService = AnomalyDetectionWindowServiceImpl.create(config, 10);
        
        assertNotNull(defaultService);
        assertNotNull(configuredService);
        assertNotNull(fullConfiguredService);
        
        // 验证配置是否正确应用
        assertEquals(config, configuredService.getConfig());
        assertEquals(config, fullConfiguredService.getConfig());
    }
    
    @Test
    void shouldSupportMethodChaining() {
        // 测试链式方法调用
        AnomalyDetectionWindowServiceImpl chainedService = new AnomalyDetectionWindowServiceImpl()
                .setConfig(config)
                .updateConfig(new AnomalyDetectionConfig());
        
        assertNotNull(chainedService);
        assertNotNull(chainedService.getConfig());
    }
} 