package com.monitor.anomaly.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.monitor.anomaly.config.DeclineDetectionConfig;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.AlertType;
import com.monitor.anomaly.service.impl.DeclineDetectionServiceImpl;
import com.monitor.anomaly.util.DataWindow;

/**
 * 下跌检测服务集成测试类
 * 测试服务与其他组件的集成情况
 */
@SpringBootTest
public class DeclineDetectionServiceIntegrationTest {

    @Autowired
    private DeclineDetectionService declineDetectionService;

    private DeclineDetectionConfig customConfig;
    
    @BeforeEach
    public void setup() {
        // 创建更宽松的自定义配置
        customConfig = new DeclineDetectionConfig();
        customConfig.setSuddenDropChangePercentThreshold(5.0); // 仅需5%的下跌
        customConfig.setSteadyDeclineTotalChangeThreshold(10.0); // 仅需10%的总下跌
        customConfig.setSteadyDeclineRSquaredThreshold(0.5); // 放宽R²要求
    }
    
    /**
     * 测试真实场景：CPU使用率突然下跌
     */
    @Test
    public void testRealWorldScenario_CPUUsageSuddenDrop() {
        // CPU使用率数据，百分比
        List<Double> cpuUsage = Arrays.asList(78.5, 82.3, 79.8, 85.1, 80.5, 83.2, 45.6);
        
        // 执行检测
        AlertReport report = declineDetectionService.detectWithValues(cpuUsage);
        
        // 断言 - 应该检测到单日暴跌
        assertNotNull(report);
        assertEquals(AlertType.SINGLE_DAY_DROP, report.getAlertType());
        assertTrue(report.isAlert());
    }
    
    /**
     * 测试真实场景：内存使用持续下降
     */
    @Test
    public void testRealWorldScenario_MemoryUsageSteadyDecline() {
        // 内存使用率数据，百分比，持续下降
        List<Double> memoryUsage = Arrays.asList(92.5, 88.7, 84.2, 79.5, 75.1, 70.8, 65.3);
        
        // 执行检测
        AlertReport report = declineDetectionService.detectWithValues(memoryUsage);
        
        // 断言 - 应该检测到持续下降
        assertNotNull(report);
        assertEquals(AlertType.STEADY_DECLINE, report.getAlertType());
        assertTrue(report.isAlert());
    }
    
    /**
     * 测试真实场景：网络流量平稳波动
     */
    @Test
    public void testRealWorldScenario_NetworkTrafficStable() {
        // 网络流量数据，Mbps，平稳波动
        List<Double> networkTraffic = Arrays.asList(145.2, 152.8, 148.9, 153.5, 147.2, 151.8, 149.5);
        
        // 执行检测
        AlertReport report = declineDetectionService.detectWithValues(networkTraffic);
        
        // 断言 - 不应该检测到异常
        assertNotNull(report);
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
        assertFalse(report.isAlert());
    }
    
    /**
     * 测试真实场景：交易量持续下降带自定义配置
     */
    @Test
    public void testRealWorldScenario_TransactionVolumeDeclineWithCustomConfig() {
        // 交易量数据，持续轻微下降
        List<Double> transactionVolume = Arrays.asList(1250.0, 1230.0, 1210.0, 1190.0, 1170.0, 1150.0, 1130.0);
        
        // 使用默认配置
        AlertReport defaultReport = declineDetectionService.detectWithValues(transactionVolume);
        
        // 使用自定义配置
        AlertReport customReport = declineDetectionService.detectWithValues(transactionVolume, customConfig);
        
        // 断言 - 默认配置不应该检测到异常（下降幅度不足）
        assertEquals(AlertType.NO_ISSUE, defaultReport.getAlertType());
        assertFalse(defaultReport.isAlert());
        
        // 断言 - 自定义配置应该检测到持续下降（更敏感）
        assertEquals(AlertType.STEADY_DECLINE, customReport.getAlertType());
        assertTrue(customReport.isAlert());
    }
    
    /**
     * 测试大型数据集
     */
    @Test
    public void testLargeDataset() {
        // 创建一个包含30天数据的较大数据集，故意添加持续下降趋势
        List<Double> largeDataset = IntStream.range(0, 30)
                .mapToDouble(i -> 1000.0 - (i * 10.0))
                .boxed()
                .collect(Collectors.toList());
        
        // 执行检测
        AlertReport report = declineDetectionService.detectWithValues(largeDataset);
        
        // 断言 - 应该检测到持续下降
        assertNotNull(report);
        assertEquals(AlertType.STEADY_DECLINE, report.getAlertType());
        assertTrue(report.isAlert());
        
        // 检查服务是否能正确处理大型窗口
        DataWindow largeWindow = new DataWindow(30);
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < 30; i++) {
            largeWindow.addDataPoint(today.minusDays(29 - i), largeDataset.get(i));
        }
        
        // 使用数据窗口执行检测
        AlertReport windowReport = declineDetectionService.detectDecline(largeWindow);
        
        // 断言结果应该相同
        assertEquals(report.getAlertType(), windowReport.getAlertType());
        assertEquals(report.isAlert(), windowReport.isAlert());
    }
} 