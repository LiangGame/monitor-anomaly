package com.monitor.anomaly.service;

import com.monitor.anomaly.config.AnomalyDetectionConfig;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.AlertType;
import com.monitor.anomaly.service.impl.AnomalyDetectionWindowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 测试用户提供的样本数据
 */
public class AnomalyDetectionSamplesTest {

    private AnomalyDetectionWindowServiceImpl service;
    private AnomalyDetectionConfig config;

    @BeforeEach
    public void setup() {
        // 创建优化后的配置，调整相关参数避免误报
        config = new AnomalyDetectionConfig();
        
        // // 调整渐变上涨检测参数
        // config.setGradualIncreaseSlopeThreshold(0.35);  // 提高斜率阈值，要求更明显的上涨趋势
        // config.setGradualIncreaseMinRSquared(0.75);     // 要求更高的拟合度
        // config.setGradualIncreaseMinConsecutiveIncreases(4); // 要求更多连续上涨天数
        // config.setGradualIncreaseTotalChangePercentThreshold(150.0); // 提高总体变化阈值
        
        // // 调整周期性波动检测参数
        // config.setPeriodicityAutocorrelationThreshold(0.8); // 提高周期性判定标准
        
        // // 调整评分权重
        // config.setScoreGradualIncreaseWeight(3.0);      // 降低渐变上涨的权重
        
        service = new AnomalyDetectionWindowServiceImpl(config);
    }

    @Test
    public void testSampleData1() {
        // 测试数据：216,578,276,418（周期性波动）
        List<Double> values = Arrays.asList(216.0, 578.0, 276.0, 418.0);
        AlertReport report = service.detectWithValues(values);
        
        System.out.println("Sample 1 - Is Alert: " + report.isAlert() 
            + ", Type: " + report.getAlertType()
            + ", Score: " + report.getTotalScore()
            + ", Description: " + report.getDescription());
        
        assertFalse(report.isAlert(), "Sample 1 should not trigger an alert");
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
    }

    @Test
    public void testSampleData2() {
        // 测试数据：309,650,555（T3没上涨）
        List<Double> values = Arrays.asList(309.0, 650.0, 555.0);
        AlertReport report = service.detectWithValues(values);
        
        System.out.println("Sample 2 - Is Alert: " + report.isAlert() 
            + ", Type: " + report.getAlertType()
            + ", Score: " + report.getTotalScore()
            + ", Description: " + report.getDescription());
        
        assertFalse(report.isAlert(), "Sample 2 should not trigger an alert");
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
    }

    @Test
    public void testSampleData3() {
        // 测试数据：245,397,294,195,203,120,293（周期性波动）
        List<Double> values = Arrays.asList(245.0, 397.0, 294.0, 195.0, 203.0, 120.0, 293.0);
        AlertReport report = service.detectWithValues(values);
        
        System.out.println("Sample 3 - Is Alert: " + report.isAlert() 
            + ", Type: " + report.getAlertType()
            + ", Score: " + report.getTotalScore()
            + ", Description: " + report.getDescription());
        
        assertFalse(report.isAlert(), "Sample 3 should not trigger an alert");
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
    }

    @Test
    public void testSampleData4() {
        // 测试数据：220,402,680,364（T4没上涨）
        List<Double> values = Arrays.asList(220.0, 402.0, 680.0, 364.0);
        AlertReport report = service.detectWithValues(values);
        
        System.out.println("Sample 4 - Is Alert: " + report.isAlert() 
            + ", Type: " + report.getAlertType()
            + ", Score: " + report.getTotalScore()
            + ", Description: " + report.getDescription());
        
        assertFalse(report.isAlert(), "Sample 4 should not trigger an alert");
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
    }

    @Test
    public void testSampleData5() {
        // 测试数据：223,252,769,1119,878,757,411（T7没上涨）
        List<Double> values = Arrays.asList(223.0, 252.0, 769.0, 1119.0, 878.0, 757.0, 411.0);
        AlertReport report = service.detectWithValues(values);
        
        System.out.println("Sample 5 - Is Alert: " + report.isAlert() 
            + ", Type: " + report.getAlertType()
            + ", Score: " + report.getTotalScore()
            + ", Description: " + report.getDescription());
        
        assertFalse(report.isAlert(), "Sample 5 should not trigger an alert");
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
    }
    
    @Test
    public void testWithDefaultConfig() {
        // 使用默认配置测试，看看是否会产生误报
        AnomalyDetectionWindowServiceImpl defaultService = new AnomalyDetectionWindowServiceImpl();
        
        System.out.println("\n使用默认配置测试样本数据：");
        
        // 测试样本1
        List<Double> values1 = Arrays.asList(216.0, 578.0, 276.0, 418.0);
        AlertReport report1 = defaultService.detectWithValues(values1);
        System.out.println("默认配置 Sample 1 - Is Alert: " + report1.isAlert() 
            + ", Type: " + report1.getAlertType()
            + ", Score: " + report1.getTotalScore());
        
        // 测试样本5
        List<Double> values5 = Arrays.asList(223.0, 252.0, 769.0, 1119.0, 878.0, 757.0, 411.0);
        AlertReport report5 = defaultService.detectWithValues(values5);
        System.out.println("默认配置 Sample 5 - Is Alert: " + report5.isAlert() 
            + ", Type: " + report5.getAlertType()
            + ", Score: " + report5.getTotalScore());
    }

    @Test
    public void testLastDayDeclineData() {
        // 测试结尾下跌的情况：开始上涨然后下跌
        List<Double> values = Arrays.asList(100.0, 150.0, 200.0, 250.0, 200.0);
        AlertReport report = service.detectWithValues(values);
        
        System.out.println("结尾下跌 - Is Alert: " + report.isAlert() 
            + ", Type: " + report.getAlertType()
            + ", Score: " + report.getTotalScore()
            + ", Description: " + report.getDescription());
        
        assertFalse(report.isAlert(), "结尾下跌的数据不应触发告警");
        assertEquals(AlertType.NO_ISSUE, report.getAlertType());
        
        // 测试单日暴涨后下跌的情况
        List<Double> values2 = Arrays.asList(100.0, 100.0, 100.0, 250.0, 200.0);
        AlertReport report2 = service.detectWithValues(values2);
        
        System.out.println("单日暴涨后下跌 - Is Alert: " + report2.isAlert() 
            + ", Type: " + report2.getAlertType()
            + ", Score: " + report2.getTotalScore()
            + ", Description: " + report2.getDescription());
        
        assertFalse(report2.isAlert(), "单日暴涨后下跌的数据不应触发告警");
        assertEquals(AlertType.NO_ISSUE, report2.getAlertType());
    }

    @Test
    public void testPeriodicityDetection() {
        // 重新创建服务实例，确保使用最新的配置
        AnomalyDetectionConfig periodicConfig = new AnomalyDetectionConfig();
        // periodicConfig.setPeriodicityAutocorrelationThreshold(0.6);
        AnomalyDetectionWindowServiceImpl periodicService = new AnomalyDetectionWindowServiceImpl(periodicConfig);
        
        // 测试用户提供的周期性波动数据 - 末尾有暴涨
        List<Double> values = Arrays.asList(245.0, 397.0, 294.0, 195.0, 203.0, 120.0, 293.0);
        AlertReport report = periodicService.detectWithValues(values);
        
        System.out.println("周期性波动测试(末尾暴涨) - Is Alert: " + report.isAlert() 
            + ", Type: " + report.getAlertType()
            + ", Score: " + report.getTotalScore()
            + ", Description: " + report.getDescription());
        
        // 这组数据应该被识别为周期性波动，但视为正常情况
        assertEquals(AlertType.NO_ISSUE, report.getAlertType(), 
                     "周期性波动应被视为正常情况");
        assertFalse(report.isAlert(), "周期性波动不应触发告警");
        
        // 测试明显的周期性波动数据
        List<Double> values2 = Arrays.asList(100.0, 150.0, 90.0, 160.0, 80.0, 170.0, 95.0);
        AlertReport report2 = periodicService.detectWithValues(values2);
        
        System.out.println("明显周期性波动测试 - Is Alert: " + report2.isAlert() 
            + ", Type: " + report2.getAlertType()
            + ", Score: " + report2.getTotalScore()
            + ", Description: " + report2.getDescription());
        
        assertEquals(AlertType.NO_ISSUE, report2.getAlertType());
        assertFalse(report2.isAlert(), "周期性波动不应触发告警");
        
        // 测试有多次方向变化但末尾不暴涨的数据
        List<Double> values3 = Arrays.asList(220.0, 180.0, 250.0, 210.0, 240.0, 180.0, 210.0);
        AlertReport report3 = periodicService.detectWithValues(values3);
        
        System.out.println("多次方向变化测试 - Is Alert: " + report3.isAlert() 
            + ", Type: " + report3.getAlertType()
            + ", Score: " + report3.getTotalScore()
            + ", Description: " + report3.getDescription());
        
        assertEquals(AlertType.NO_ISSUE, report3.getAlertType());
        assertFalse(report3.isAlert(), "周期性波动不应触发告警");
    }
} 