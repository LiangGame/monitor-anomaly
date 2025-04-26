package com.monitor.anomaly.service.impl;

import com.monitor.anomaly.config.DeclineDetectionConfig;
import com.monitor.anomaly.dto.DataPointDTO;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.AlertType;
import com.monitor.anomaly.service.DeclineDetectionService;
import com.monitor.anomaly.util.DataWindow;
import com.monitor.anomaly.util.StatisticsUtil;
import com.monitor.anomaly.util.StatisticsUtil.LinearRegression;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 下跌检测服务的实现
 * 专注于检测数据的下降趋势
 * 完全独立于任何框架，可在任何Java环境中使用
 */
@Service
@Slf4j
public class DeclineDetectionServiceImpl implements DeclineDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(DeclineDetectionServiceImpl.class);
    
    private final DeclineDetectionConfig defaultConfig = new DeclineDetectionConfig();
    private DeclineDetectionConfig config;
    private final DataWindow dataWindow;
    
    // 默认窗口大小
    private static final int DEFAULT_WINDOW_SIZE = 7;
    
    /**
     * 创建默认实例
     */
    public static DeclineDetectionServiceImpl createDefault() {
        return new DeclineDetectionServiceImpl();
    }
    
    /**
     * 创建带配置的实例
     */
    public static DeclineDetectionServiceImpl create(DeclineDetectionConfig config) {
        return new DeclineDetectionServiceImpl(config);
    }
    
    /**
     * 创建带配置和窗口大小的实例
     */
    public static DeclineDetectionServiceImpl create(DeclineDetectionConfig config, int windowSize) {
        return new DeclineDetectionServiceImpl(config, windowSize);
    }
    
    /**
     * 默认构造函数
     */
    public DeclineDetectionServiceImpl() {
        this(new DeclineDetectionConfig(), DEFAULT_WINDOW_SIZE);
    }
    
    /**
     * 带配置的构造函数
     */
    public DeclineDetectionServiceImpl(DeclineDetectionConfig config) {
        this(config, DEFAULT_WINDOW_SIZE);
    }
    
    /**
     * 完整构造函数
     */
    public DeclineDetectionServiceImpl(DeclineDetectionConfig config, int windowSize) {
        this.config = config != null ? config : new DeclineDetectionConfig();
        this.dataWindow = new DataWindow(windowSize > 0 ? windowSize : DEFAULT_WINDOW_SIZE);
    }
    
    /**
     * 设置配置（支持链式调用）
     */
    public DeclineDetectionServiceImpl setConfig(DeclineDetectionConfig config) {
        this.config = config != null ? config : new DeclineDetectionConfig();
        return this;
    }
    
    /**
     * 更新配置（支持链式调用）
     */
    public DeclineDetectionServiceImpl updateConfig(DeclineDetectionConfig newConfig) {
        if (newConfig != null) {
            this.config = this.config.merge(newConfig);
        }
        return this;
    }
    
    /**
     * 获取当前配置
     */
    public DeclineDetectionConfig getConfig() {
        return this.config;
    }
    
    /**
     * 获取当前数据窗口
     */
    public DataWindow getDataWindow() {
        return this.dataWindow;
    }
    
    /**
     * 创建临时数据窗口
     */
    protected DataWindow createTemporaryWindow(int size) {
        return new DataWindow(Math.max(DEFAULT_WINDOW_SIZE, size));
    }
    
    @Override
    public AlertReport detectDecline(DataWindow dataWindow) {
        return detectDecline(dataWindow, null);
    }
    
    @Override
    public AlertReport detectDecline(DataWindow dataWindow, DeclineDetectionConfig customConfig) {
        // 合并配置
        DeclineDetectionConfig config = defaultConfig.merge(customConfig);
        
        // 日志记录使用的配置参数
        logger.debug("使用的检测配置 - 日均下降阈值: {}%, 总体变化阈值: {}%, R²阈值: {}, 最小连续下降天数: {}", 
            config.getSteadyDailyAverageDeclineThreshold(), 
            config.getSteadyDeclineTotalChangeThreshold(),
            config.getSteadyDeclineRSquaredThreshold(),
            config.getSteadyDeclineMinConsecutiveDays());
        
        // 检查数据点数量是否充足
        if (dataWindow == null || dataWindow.size() < 2) {
            logger.debug("数据点不足，无法进行下跌检测");
            return createNormalReport("unknown");
        }
        
        // 提取数据值
        double[] values = dataWindow.getValues();
        
        // 记录基本统计信息
        double mean = StatisticsUtil.mean(values);
        double stdDev = StatisticsUtil.standardDeviation(values);
        logger.debug("检测数据窗口 - 均值: {}, 标准差: {}", mean, stdDev);
        
        // 检测突然暴跌
        AlertReport suddenDropReport = detectSuddenDrop("unknown", dataWindow, config);
        if (suddenDropReport.getAlertType() != AlertType.NO_ISSUE) {
            return suddenDropReport;
        }
        
        // 检测持续下降
        AlertReport steadyDeclineReport = detectSteadyDecline("unknown", dataWindow, config);
        if (steadyDeclineReport.getAlertType() != AlertType.NO_ISSUE) {
            return steadyDeclineReport;
        }
        
        // 未检测到异常
        return createNormalReport("unknown");
    }
    
    /**
     * 检测突然暴跌
     */
    private AlertReport detectSuddenDrop(String metricName, DataWindow dataWindow, DeclineDetectionConfig config) {
        // 确保至少有两个数据点
        if (dataWindow.size() < 2) {
            return createNormalReport(metricName);
        }
        
        double[] valuesArray = dataWindow.getValues();
        List<Double> values = Arrays.stream(valuesArray).boxed().collect(Collectors.toList());
        
        // 计算基本统计量
        double mean = calculateMean(valuesArray);
        double stdDev = calculateStandardDeviation(valuesArray, mean);
        
        // 获取最近两个数据点
        double current = values.get(values.size() - 1);
        double previous = values.get(values.size() - 2);
        
        // 计算变化
        double absoluteChange = current - previous;
        double changePercent = calculateChangePercent(previous, current);
        
        // 计算当前值相对于平均值偏离的标准差倍数
        double deviationFromMean = stdDev > 0 ? (current - mean) / stdDev : 0;
        
        // 记录检测统计信息
        logger.debug("突然暴跌检测 - 当前值: {}, 前一值: {}, 变化百分比: {}%, 偏离均值标准差倍数: {}, 绝对变化: {}", 
                current, previous, changePercent, deviationFromMean, absoluteChange);
        
        // 首先检查绝对变化是否满足最小要求
        if (Math.abs(absoluteChange) <= config.getSuddenDropMinAbsoluteChange()) {
            logger.debug("绝对变化 {} 小于或等于最小阈值 {}，不满足突然暴跌条件", 
                    Math.abs(absoluteChange), config.getSuddenDropMinAbsoluteChange());
            return createNormalReport(metricName);
        }
        
        // 判断是否为暴跌 - 需满足百分比变化或标准差偏离条件
        boolean isSuddenDrop = changePercent < -config.getSuddenDropChangePercentThreshold() 
                || deviationFromMean < -config.getSuddenDropStdDeviationMultiplier();
        
        if (isSuddenDrop) {
            // 计算严重程度分数 - 取百分比变化和标准差倍数的最大比例
            double severityScore = Math.max(
                Math.abs(changePercent) / config.getSuddenDropChangePercentThreshold(),
                Math.abs(deviationFromMean) / config.getSuddenDropStdDeviationMultiplier()
            );
            
            // 限制严重程度在0-1范围内，并乘以权重
            severityScore = Math.min(1.0, severityScore) * config.getSuddenDropWeight();
            
            StringBuilder description = new StringBuilder();
            
            if (changePercent < -config.getSuddenDropChangePercentThreshold()) {
                description.append(String.format("单日下跌%.2f%%", Math.abs(changePercent)));
            }
            
            if (deviationFromMean < -config.getSuddenDropStdDeviationMultiplier()) {
                if (description.length() > 0) {
                    description.append("，");
                }
                description.append(String.format("较前期平均值偏离%.2f个标准差", Math.abs(deviationFromMean)));
            }
            
            // 创建告警报告
            return AlertReport.builder()
                    .alertType(AlertType.SINGLE_DAY_DROP)
                    .date(LocalDate.now())
                    .totalScore(severityScore)
                    .description(description.toString())
                    .isAlert(true)
                    .build();
        }
        
        return createNormalReport(metricName);
    }
    
    /**
     * 计算数组的平均值
     */
    private double calculateMean(double[] values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        
        return sum / values.length;
    }
    
    /**
     * 计算数组的标准差
     */
    private double calculateStandardDeviation(double[] values, double mean) {
        if (values == null || values.length <= 1) {
            return 0;
        }
        
        double sumSquaredDiff = 0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        
        return Math.sqrt(sumSquaredDiff / (values.length - 1));
    }
    
    /**
     * 检测持续下降
     */
    private AlertReport detectSteadyDecline(String metricName, DataWindow dataWindow, DeclineDetectionConfig config) {
        // 确保数据点足够进行趋势分析
        if (dataWindow.size() < config.getSteadyDeclineMinDataPoints()) {
            return createNormalReport(metricName);
        }
        
        double[] valuesArray = dataWindow.getValues();
        List<Double> values = Arrays.stream(valuesArray).boxed().collect(Collectors.toList());
        LocalDate[] datesArray = dataWindow.getDates();
        List<LocalDateTime> dates = Arrays.stream(datesArray)
                .map(date -> date.atStartOfDay())
                .collect(Collectors.toList());
        
        // 计算首尾绝对变化值
        double firstValue = values.get(0);
        double lastValue = values.get(values.size() - 1);
        double absoluteTotalChange = Math.abs(lastValue - firstValue);
        
        // 首先检查总体绝对变化是否满足最小要求
        if (absoluteTotalChange <= config.getSuddenDropMinAbsoluteChange()) {
            logger.debug("总体绝对变化 {} 小于或等于最小阈值 {}，不满足持续下降条件", 
                    absoluteTotalChange, config.getSuddenDropMinAbsoluteChange());
            return createNormalReport(metricName);
        }
        
        // 执行线性回归分析
        double[] x = new double[values.size()];
        double[] y = new double[values.size()];
        
        for (int i = 0; i < values.size(); i++) {
            // 使用日期索引作为x值（简化计算）
            x[i] = i;
            y[i] = values.get(i);
        }
        
        // 创建线性回归实例
        LinearRegression regression = new LinearRegression();
        regression.addData(x, y);
        
        // 计算斜率（负斜率表示下降趋势）
        double slope = regression.getSlope();
        
        // 计算R²（确定系数，表示趋势线的拟合程度）
        double rSquared = regression.getRSquare();
        
        // 计算总体变化百分比（从第一个数据点到最后一个）
        double totalChangePercent = calculateChangePercent(firstValue, lastValue);
        
        // 统计下降天数、连续下降天数
        int downDays = 0;
        int consecutiveDeclines = 0;
        int maxConsecutiveDeclines = 0;
        
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i-1)) {
                downDays++; // 统计总下降天数
                consecutiveDeclines++;
                maxConsecutiveDeclines = Math.max(maxConsecutiveDeclines, consecutiveDeclines);
            } else {
                consecutiveDeclines = 0;
            }
        }
        
        // 计算每日变化百分比
        double[] dailyChanges = new double[values.size() - 1];
        for (int i = 1; i < values.size(); i++) {
            dailyChanges[i-1] = calculateChangePercent(values.get(i-1), values.get(i));
        }
        
        // 计算平均日下降幅度（仅计算下降日）
        double avgDailyDeclinePercent = 0;
        if (downDays > 0) {
            double totalDeclinePercent = 0;
            int declineDays = 0;
            for (double change : dailyChanges) {
                if (change < 0) {
                    totalDeclinePercent += change; // 累加负值
                    declineDays++;
                }
            }
            avgDailyDeclinePercent = declineDays > 0 ? totalDeclinePercent / declineDays : 0;
        }
        
        // 记录检测统计信息
        logger.debug("持续下降检测 - 斜率: {}, R²: {}, 最大连续下降天数: {}, 总体变化: {}%, 总体绝对变化: {}, 下降天数: {}/{}, 平均日下降: {}%", 
                slope, rSquared, maxConsecutiveDeclines, totalChangePercent, absoluteTotalChange, downDays, values.size(), avgDailyDeclinePercent);
        
        // 记录条件判断详情
        // 条件1：传统线性回归判断 - 负斜率、良好拟合度、连续下降天数达标
        boolean condition1 = slope < 0 
                && rSquared > config.getSteadyDeclineRSquaredThreshold()
                && maxConsecutiveDeclines >= config.getSteadyDeclineMinConsecutiveDays();
        
        // 条件2：基于总体变化百分比判断 - 大幅下降且一定的拟合度
        boolean condition2 = totalChangePercent < -config.getSteadyDeclineTotalChangeThreshold()
                && rSquared > 0.5; // 适当放宽R²要求，但仍需保持一定拟合度
        
        // 条件3：基于下降天数和平均日下降幅度判断 - 适用于间歇性下降
        boolean condition3 = downDays >= (values.size() / 2) // 至少一半天数在下降
                && avgDailyDeclinePercent < -config.getSteadyDailyAverageDeclineThreshold()
                && totalChangePercent < -config.getSteadyDeclineTotalChangeThreshold() / 2; // 总降幅不能太小
        
        logger.debug("持续下降条件判断 - 条件1(传统): {}, 条件2(总体变化): {}, 条件3(间歇式): {}", 
                condition1, condition2, condition3);
        
        // 满足任一条件即可判定为持续下降
        boolean isSteadyDecline = condition1 || condition2 || condition3;
        
        if (isSteadyDecline) {
            StringBuilder description = new StringBuilder();
            
            // 计算严重程度分数
            double severityScore1 = condition1 ? rSquared : 0;
            double severityScore2 = condition2 ? Math.min(1.0, Math.abs(totalChangePercent) / (2 * config.getSteadyDeclineTotalChangeThreshold())) : 0;
            double severityScore3 = condition3 ? Math.min(1.0, Math.abs(avgDailyDeclinePercent) / (2 * config.getSteadyDailyAverageDeclineThreshold())) : 0;
            double severityScore = Math.max(Math.max(severityScore1, severityScore2), severityScore3);
            
            // 限制分数在0-1范围内，并乘以权重得到最终分数
            severityScore = Math.min(1.0, severityScore) * config.getSteadyDeclineWeight();
            
            // 构建描述文本
            if (condition1) {
                description.append(String.format("检测到连续下降趋势，最大连续下降%d天，日均变化率%.2f%%，拟合度R²=%.2f", 
                        maxConsecutiveDeclines, slope * 100, rSquared));
            } else if (condition2) {
                description.append(String.format("检测到大幅累计下降，总降幅%.2f%%，拟合度R²=%.2f", 
                        totalChangePercent, rSquared));
            } else {
                description.append(String.format("检测到间歇性下降，%d天中有%d天下降，平均日降幅%.2f%%，总降幅%.2f%%", 
                        values.size(), downDays, avgDailyDeclinePercent, totalChangePercent));
            }
            
            // 创建告警报告
            return AlertReport.builder()
                    .alertType(AlertType.STEADY_DECLINE)
                    .date(LocalDate.now())
                    .totalScore(severityScore)
                    .description(description.toString())
                    .isAlert(true)
                    .build();
        }
        
        return createNormalReport(metricName);
    }
    
    /**
     * 计算变化百分比
     */
    private double calculateChangePercent(double from, double to) {
        if (Math.abs(from) < 0.00001) {
            // 避免除以零或非常小的值，使用一个小的值替代
            from = 0.00001;
        }
        return ((to - from) / Math.abs(from)) * 100;
    }
    
    /**
     * 创建正常报告（无异常）
     */
    private AlertReport createNormalReport(String metricName) {
        return AlertReport.builder()
                .alertType(AlertType.NO_ISSUE)
                .date(LocalDate.now())
                .totalScore(0.0)
                .description("未检测到异常")
                .isAlert(false)
                .build();
    }
    
    @Override
    public AlertReport addPointAndDetect(DataWindow dataWindow, Date date, double value) {
        return addPointAndDetect(dataWindow, date, value, null);
    }
    
    @Override
    public AlertReport addPointAndDetect(DataWindow dataWindow, Date date, double value, DeclineDetectionConfig customConfig) {
        if (dataWindow == null || date == null) {
            return createNormalReport("unknown");
        }
        
        // 转换日期为LocalDate
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        // 添加数据点
        dataWindow.addDataPoint(localDate, value);
        
        // 执行异常检测
        return detectDecline(dataWindow, customConfig);
    }
    
    @Override
    public AlertReport detectWithValues(List<Double> values) {
        return detectWithValues(values, null);
    }
    
    @Override
    public AlertReport detectWithValues(List<Double> values, DeclineDetectionConfig customConfig) {
        if (values == null || values.isEmpty()) {
            return createNormalReport("unknown");
        }
        
        // 将值转换为带日期的数据点
        List<DataPointDTO> dataPoints = convertValuesToDataPoints(values);
        
        // 使用转换后的数据点进行检测
        return detectWithDataPoints(dataPoints, customConfig);
    }
    
    @Override
    public AlertReport detectDeclineWithValues(List<Double> values) {
        return detectWithValues(values);
    }
    
    @Override
    public AlertReport detectDeclineWithValues(List<Double> values, DeclineDetectionConfig customConfig) {
        return detectWithValues(values, customConfig);
    }
    
    @Override
    public AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints) {
        return detectWithDataPoints(dataPoints, null);
    }
    
    @Override
    public AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints, DeclineDetectionConfig customConfig) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return createNormalReport("unknown");
        }
        
        // 创建临时窗口
        DataWindow tempWindow = createTemporaryWindow(dataPoints.size());
        
        // 按日期排序并添加数据点
        dataPoints.stream()
                .sorted(Comparator.comparing(DataPointDTO::getDate))
                .forEach(point -> {
                    LocalDate date = point.getDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    tempWindow.addDataPoint(date, point.getValue());
                });
        
        log.info("批量处理数据点: {} 个点已添加到临时窗口", tempWindow.size());
        
        // 使用临时窗口检测
        return detectDecline(tempWindow, customConfig);
    }
    
    /**
     * 将纯值列表转换为带日期的数据点列表
     */
    private List<DataPointDTO> convertValuesToDataPoints(List<Double> values) {
        List<DataPointDTO> dataPoints = new ArrayList<>();
        Date today = new Date();
        long dayInMillis = 24 * 60 * 60 * 1000L;
        
        for (int i = 0; i < values.size(); i++) {
            // 计算日期（最新值对应最近日期）
            Date date = new Date(today.getTime() - (values.size() - 1 - i) * dayInMillis);
            Double value = values.get(i);
            
            // 跳过null值
            if (value != null) {
                dataPoints.add(new DataPointDTO(date, value));
            }
        }
        
        return dataPoints;
    }
    
    /**
     * 构建器类
     */
    public static class Builder {
        private DeclineDetectionConfig config;
        private Integer windowSize;
        
        public Builder() {
            // 默认使用null，会在构建时使用默认值
        }
        
        /**
         * 设置配置
         */
        public Builder withConfig(DeclineDetectionConfig config) {
            this.config = config;
            return this;
        }
        
        /**
         * 设置窗口大小
         */
        public Builder withWindowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }
        
        /**
         * 构建服务实例
         */
        public DeclineDetectionServiceImpl build() {
            if (windowSize != null && config != null) {
                return new DeclineDetectionServiceImpl(config, windowSize);
            } else if (config != null) {
                return new DeclineDetectionServiceImpl(config);
            } else {
                return new DeclineDetectionServiceImpl();
            }
        }
    }
    
    /**
     * 创建构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }
} 