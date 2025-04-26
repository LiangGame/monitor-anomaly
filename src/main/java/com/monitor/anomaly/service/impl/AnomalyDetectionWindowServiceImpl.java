package com.monitor.anomaly.service.impl;

import com.monitor.anomaly.config.AnomalyDetectionConfig;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.AlertReport.SeverityLevel;
import com.monitor.anomaly.model.AlertType;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.service.AnomalyDetectionWindowService;
import com.monitor.anomaly.util.DataWindow;
import com.monitor.anomaly.util.StatisticsUtil;
import com.monitor.anomaly.util.StatisticsUtil.DescriptiveStatistics;
import com.monitor.anomaly.util.StatisticsUtil.LinearRegression;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 基于窗口的异常检测服务实现
 * 设计为独立于任何框架的服务实现，可在Spring和非Spring环境中使用
 */
@Slf4j
public class AnomalyDetectionWindowServiceImpl implements AnomalyDetectionWindowService {

    private AnomalyDetectionConfig config;
    private final DataWindow dataWindow;
    
    // 默认窗口大小
    private static final int DEFAULT_WINDOW_SIZE = 7;

    /**
     * 创建一个带默认配置和默认窗口大小的异常检测服务
     * 
     * @return 新的异常检测服务实例
     */
    public static AnomalyDetectionWindowServiceImpl createDefault() {
        return new AnomalyDetectionWindowServiceImpl();
    }
    
    /**
     * 创建一个带自定义配置和默认窗口大小的异常检测服务
     * 
     * @param config 自定义配置
     * @return 新的异常检测服务实例
     */
    public static AnomalyDetectionWindowServiceImpl create(AnomalyDetectionConfig config) {
        return new AnomalyDetectionWindowServiceImpl(config);
    }
    
    /**
     * 创建一个带自定义配置和自定义窗口大小的异常检测服务
     * 
     * @param config 自定义配置
     * @param windowSize 数据窗口大小
     * @return 新的异常检测服务实例
     */
    public static AnomalyDetectionWindowServiceImpl create(AnomalyDetectionConfig config, int windowSize) {
        return new AnomalyDetectionWindowServiceImpl(config, windowSize);
    }
    
    /**
     * 无参构造函数，使用默认配置和默认窗口大小
     */
    public AnomalyDetectionWindowServiceImpl() {
        this(new AnomalyDetectionConfig(), DEFAULT_WINDOW_SIZE);
    }
    
    /**
     * 带配置的构造函数，使用默认窗口大小
     * 
     * @param config 自定义配置
     */
    public AnomalyDetectionWindowServiceImpl(AnomalyDetectionConfig config) {
        this(config, DEFAULT_WINDOW_SIZE);
    }
    
    /**
     * 完整构造函数，带配置和窗口大小
     * 
     * @param config 自定义配置
     * @param windowSize 窗口大小
     */
    public AnomalyDetectionWindowServiceImpl(AnomalyDetectionConfig config, int windowSize) {
        this.config = config != null ? config : new AnomalyDetectionConfig();
        this.dataWindow = new DataWindow(windowSize > 0 ? windowSize : DEFAULT_WINDOW_SIZE);
    }
    
    /**
     * 设置配置对象（替换当前配置）
     * 
     * @param config 新配置
     * @return 当前服务实例，支持链式调用
     */
    public AnomalyDetectionWindowServiceImpl setConfig(AnomalyDetectionConfig config) {
        this.config = config != null ? config : new AnomalyDetectionConfig();
        return this;
    }
    
    /**
     * 更新配置（合并到当前配置）
     * 
     * @param newConfig 需要合并的新配置
     * @return 当前服务实例，支持链式调用
     */
    public AnomalyDetectionWindowServiceImpl updateConfig(AnomalyDetectionConfig newConfig) {
        if (newConfig != null) {
            this.config = this.config.merge(newConfig);
        }
        return this;
    }
    
    /**
     * 获取当前配置
     * 
     * @return 当前使用的配置
     */
    public AnomalyDetectionConfig getConfig() {
        return this.config;
    }
    
    /**
     * 获取当前的数据窗口
     * 
     * @return 当前使用的数据窗口
     */
    public DataWindow getDataWindow() {
        return this.dataWindow;
    }
    
    /**
     * 创建一个新的临时数据窗口
     * 
     * @param size 窗口大小
     * @return 新创建的数据窗口
     */
    protected DataWindow createTemporaryWindow(int size) {
        return new DataWindow(Math.max(DEFAULT_WINDOW_SIZE, size));
    }

    @Override
    public AlertReport detectAnomaly(DataWindow dataWindow) {
        return detectAnomaly(dataWindow, null);
    }

    @Override
    public AlertReport detectAnomaly(DataWindow dataWindow, AnomalyDetectionConfig customConfig) {
        // 保存原配置以便在方法结束时恢复
        AnomalyDetectionConfig originalConfig = this.config;
        try {
            // 如果提供了自定义配置，临时替换当前配置
            if (customConfig != null) {
                this.config = customConfig;
            }
            
            // 数据不足时无法进行检测
            if (dataWindow.size() < 3) {
                return createNormalReport(LocalDate.now(), "数据点不足，无法检测");
            }
            
            // 提取值序列
            double[] values = dataWindow.getValues();
            
            // 输出数据概况，便于调试
            DescriptiveStatistics stats = new DescriptiveStatistics(values);
            log.info("检测数据：点数={}, 均值={}, 标准差={}, 最小值={}, 最大值={}",
                    values.length, stats.getMean(), 
                    stats.getStandardDeviation(), stats.getMin(), stats.getMax());
            
            // 按照优先级顺序检测各类异常
            AlertReport report = detectAnomaliesByPriority(dataWindow, values);
            
            // 返回检测结果
            return report;
        } finally {
            // 恢复原配置
            this.config = originalConfig;
        }
    }
    
    /**
     * 按照优先级顺序检测各类异常
     */
    private AlertReport detectAnomaliesByPriority(DataWindow dataWindow, double[] values) {
        // 1. 检测暴涨（高优先级 - 权重最高）
        AlertReport suddenSpikeReport = detectSuddenSpike(dataWindow, values);
        log.info("暴涨检测结果: {}, 分数: {}", suddenSpikeReport.isAlert(), suddenSpikeReport.getTotalScore());
        
        if (suddenSpikeReport.isAlert()) {
            return calculateTotalScore(suddenSpikeReport);
        }
        
        // 2. 检测渐变上涨（中优先级 - 权重中等）
        AlertReport gradualReport = detectGradualIncrease(dataWindow, values);
        log.info("渐变上涨检测结果: {}, 分数: {}", gradualReport.isAlert(), gradualReport.getTotalScore());
        
        if (gradualReport.isAlert()) {
            return calculateTotalScore(gradualReport);
        }
        
        // 3. 检测周期性波动（低优先级 - 权重最低）
        boolean isPeriodic = hasPeriodicity(values);
        log.info("周期性检测结果: {}", isPeriodic);
        
        if (isPeriodic) {
            AlertReport periodicReport = createPeriodicReport(
                dataWindow.getDates()[dataWindow.size()-1], 
                values
            );
            log.info("创建周期性报告: {}", periodicReport);
            return periodicReport; // 直接返回，无需再次计算分数
        }
        
        // 如果都没有检测到异常，返回正常结果
        return createNormalReport(dataWindow.getDates()[dataWindow.size()-1], "未检测到异常");
    }

    @Override
    public AlertReport addPointAndDetect(LocalDate date, double value) {
        return addPointAndDetect(date, value, null);
    }

    @Override
    public AlertReport addPointAndDetect(LocalDate date, double value, AnomalyDetectionConfig customConfig) {
        // 保存原配置以便在方法结束时恢复
        AnomalyDetectionConfig originalConfig = this.config;
        try {
            // 如果提供了自定义配置，临时替换当前配置
            if (customConfig != null) {
                this.config = customConfig;
            }
            
            // 添加数据点
            dataWindow.addDataPoint(date, value);
            
            // 执行异常检测
            return detectAnomaly(dataWindow, null); // 传null因为已经替换了配置
        } finally {
            // 恢复原配置
            this.config = originalConfig;
        }
    }
    
    @Override
    public AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints) {
        return detectWithDataPoints(dataPoints, null);
    }
    
    @Override
    public AlertReport detectWithDataPoints(List<DataPointDTO> dataPoints, AnomalyDetectionConfig customConfig) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return createNormalReport(LocalDate.now(), "未提供数据点");
        }
        
        // 清空当前数据窗口
        this.dataWindow.clear();
        
        // 按日期排序并添加数据点
        dataPoints.stream()
                .sorted(Comparator.comparing(DataPointDTO::getDate))
                .forEach(point -> this.dataWindow.addDataPoint(point.getDate(), point.getValue()));
        
        log.info("批量处理数据点: {} 个点已添加到数据窗口", this.dataWindow.size());
        
        // 使用实例的数据窗口检测
        return detectAnomaly(this.dataWindow, customConfig);
    }
    
    @Override
    public AlertReport detectWithValues(List<Double> values) {
        return detectWithValues(values, null);
    }
    
    @Override
    public AlertReport detectWithValues(List<Double> values, AnomalyDetectionConfig customConfig) {
        if (values == null || values.isEmpty()) {
            return createNormalReport(LocalDate.now(), "未提供数据点");
        }
        
        // 将值转换为带日期的数据点
        List<DataPointDTO> dataPoints = convertValuesToDataPoints(values);
        
        // 使用转换后的数据点进行检测
        return detectWithDataPoints(dataPoints, customConfig);
    }
    
    /**
     * 将纯值列表转换为带日期的数据点列表
     * 假设最后一个值对应今天，逐日往前推
     */
    private List<DataPointDTO> convertValuesToDataPoints(List<Double> values) {
        List<DataPointDTO> dataPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < values.size(); i++) {
            // 计算日期（最新值对应最近日期）
            LocalDate date = today.minusDays(values.size() - 1 - i);
            Double value = values.get(i);
            
            // 跳过null值
            if (value != null) {
                dataPoints.add(new DataPointDTO(date, value));
            }
        }
        
        return dataPoints;
    }
    
    /**
     * 计算总分数并评估严重程度
     */
    private AlertReport calculateTotalScore(AlertReport report) {
        double weight = 0.0;
        
        // 根据告警类型选择对应权重
        switch (report.getAlertType()) {
            case SINGLE_DAY_SPIKE:
                weight = this.config.getScoreSuddenSpikeWeight();
                break;
            case STEADY_RISE:
                weight = this.config.getScoreGradualIncreaseWeight();
                break;
            case ABNORMAL_VOLATILITY:
                weight = this.config.getScorePeriodicWeight();
                break;
            default:
                weight = 0.0;
        }
        
        
        // 更新描述
        String descriptionPrefix = report.getAlertType().getDescription() + "：";
        if (report.getDescription() != null && !report.getDescription().startsWith(descriptionPrefix)) {
            report.setDescription(descriptionPrefix + report.getDescription());
        }
        
        // 计算总分 = 置信度 × 权重
        double totalScore = report.getTotalScore() * weight;
        report.setTotalScore(totalScore);
        
        // 评估严重程度
        if (totalScore >= this.config.getScoreCriticalThreshold()) {
            report.setSeverityLevel(SeverityLevel.CRITICAL);
        } else if (totalScore >= this.config.getScoreWarningThreshold()) {
            report.setSeverityLevel(SeverityLevel.WARNING);
        } else {
            report.setSeverityLevel(SeverityLevel.NORMAL);
            
            // 如果严重程度是NORMAL，则告警类型设为NO_ISSUE
            if (report.getSeverityLevel() == SeverityLevel.NORMAL) {
                report.setAlertType(AlertType.NO_ISSUE);
                report.setDescription("没有检测到异常");
                report.setAlert(false);
            }
        }
        
        return report;
    }
    
    /**
     * 检测渐变上涨
     */
    private AlertReport detectGradualIncrease(DataWindow dataWindow, double[] values) {
        // 获取最近日期
        LocalDate latestDate = dataWindow.getDates()[dataWindow.size()-1];
        
        // 线性回归分析
        LinearRegression regression = new LinearRegression();
        
        // 创建x和y数组
        double[] xValues = IntStream.range(0, values.length).mapToDouble(i -> (double) i).toArray();
        regression.addData(xValues, values);
        
        double slope = regression.getSlope();        // 斜率
        double rSquared = regression.getRSquare();  // 决定系数
        
        // 计算累计涨幅 - 起点和终点比较
        double firstValue = values[0];
        double lastValue = values[values.length - 1];
        double totalChangePercent = firstValue > 0 ? ((lastValue - firstValue) / firstValue * 100) : 0;
        
        // 统计上涨天数、连续上涨天数
        int upDays = 0;
        int consecutiveIncreases = 0;
        int maxConsecutiveIncreases = 0;
        
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[i-1]) {
                upDays++; // 统计总上涨天数
                consecutiveIncreases++;
                maxConsecutiveIncreases = Math.max(maxConsecutiveIncreases, consecutiveIncreases);
            } else {
                consecutiveIncreases = 0;
            }
        }
        
        // 计算上涨天的平均上涨幅度
        double[] dailyChanges = new double[values.length - 1];
        for (int i = 1; i < values.length; i++) {
            if (values[i-1] > 0) {
                dailyChanges[i-1] = (values[i] - values[i-1]) / values[i-1] * 100;
            } else {
                dailyChanges[i-1] = 0;
            }
        }
        
        // 计算平均日涨幅（仅计算上涨日）
        double avgDailyIncreasePercent = 0;
        if (upDays > 0) {
            double totalIncreasePercent = 0;
            int increaseDays = 0;
            for (double change : dailyChanges) {
                if (change > 0) {
                    totalIncreasePercent += change;
                    increaseDays++;
                }
            }
            avgDailyIncreasePercent = totalIncreasePercent / increaseDays;
        }
        
        log.debug("渐变上涨指标 - 斜率: {}, R²: {}, 最大连续上涨天数: {}, 总体变化百分比: {}%, 上涨天数: {}, 平均日涨幅: {}%", 
                slope, rSquared, maxConsecutiveIncreases, totalChangePercent, upDays, avgDailyIncreasePercent);
        
        // 判断条件1：基于线性回归的传统判断
        boolean condition1 = slope > this.config.getGradualIncreaseSlopeThreshold() 
                && rSquared > this.config.getGradualIncreaseMinRSquared()
                && maxConsecutiveIncreases >= this.config.getGradualIncreaseMinConsecutiveIncreases();
        
        // 判断条件2：基于累计涨幅的判断
        boolean condition2 = totalChangePercent >= this.config.getGradualIncreaseTotalChangePercentThreshold()
                && rSquared > 0.5; // 仍然要求一定的拟合度，避免剧烈波动被误判
        
        // 判断条件3：基于平均日涨幅的判断（适用于间歇性上涨模式）
        boolean condition3 = upDays >= (values.length / 2)  // 至少一半天数在上涨
                && avgDailyIncreasePercent >= this.config.getGradualIncreaseSlopeThreshold() * 100  // 平均日涨幅显著
                && totalChangePercent >= this.config.getGradualIncreaseTotalChangePercentThreshold() / 2;  // 总涨幅不能太小
        
        log.debug("渐变上涨条件检查 - 条件1: {}, 条件2: {}, 条件3: {}", condition1, condition2, condition3);
        
        // 满足任一条件即可判定为渐变上涨
        boolean isGradualIncrease = condition1 || condition2 || condition3;
        
        if (isGradualIncrease) {
            StringBuilder description = new StringBuilder();
            
            // 计算置信度分数
            double confidenceScore1 = condition1 ? rSquared : 0;
            double confidenceScore2 = condition2 ? Math.min(1.0, totalChangePercent / (2 * this.config.getGradualIncreaseTotalChangePercentThreshold())) : 0;
            double confidenceScore3 = condition3 ? Math.min(1.0, avgDailyIncreasePercent / (2 * this.config.getGradualIncreaseSlopeThreshold() * 100)) : 0;
            double confidenceScore = Math.max(Math.max(confidenceScore1, confidenceScore2), confidenceScore3);
            
            // 限制置信度在0-1范围内
            confidenceScore = Math.min(1.0, confidenceScore);
            
            // 构建描述
            if (condition1) {
                description.append(String.format("检测到连续上涨趋势，最大连续上涨%d天，日均增长率%.2f%%，拟合度R²=%.2f", 
                        maxConsecutiveIncreases, slope * 100, rSquared));
            } else if (condition2) {
                description.append(String.format("检测到大幅累计上涨，总涨幅%.2f%%，拟合度R²=%.2f", 
                        totalChangePercent, rSquared));
            } else {
                description.append(String.format("检测到间歇性上涨，%d天中有%d天上涨，平均日涨幅%.2f%%，总涨幅%.2f%%", 
                        values.length, upDays, avgDailyIncreasePercent, totalChangePercent));
            }
            
            return AlertReport.builder()
                    .date(latestDate)
                    .isAlert(true)
                    .alertType(AlertType.STEADY_RISE)
                    .totalScore(confidenceScore)
                    .description(description.toString())
                    .build();
        }
        
        return createNormalReport(latestDate, "未检测到稳定上涨");
    }

    /**
     * 检测暴涨
     */
    private AlertReport detectSuddenSpike(DataWindow dataWindow, double[] values) {
        // 获取最近日期
        LocalDate latestDate = dataWindow.getDates()[dataWindow.size()-1];
        
        DescriptiveStatistics stats = new DescriptiveStatistics(values);
        
        // 只检查最后一天是否有暴涨
        if (values.length > 1) {
            double lastValue = values[values.length - 1];
            double previousValue = values[values.length - 2];
            
            // 计算相对变化
            double absoluteChange = lastValue - previousValue;
            double percentageChange = previousValue != 0 ? (absoluteChange / previousValue) * 100 : 0;
            
            // 计算标准差倍数
            double mean = stats.getMean();
            double stdDev = stats.getStandardDeviation();
            // 避免除以零
            double deviationFromMean = stdDev > 0 ? (lastValue - mean) / stdDev : 0;
            
            // 记录计算结果用于调试
            log.debug("Sudden spike detection - percentageChange: {}, deviationFromMean: {}, absoluteChange: {}", 
                    percentageChange, deviationFromMean, absoluteChange);
            
            // 判断是否为暴涨 - 使用或条件而不是与条件，更宽松的判断
            boolean isSuddenSpike = (percentageChange > this.config.getSuddenSpikePercentageChangeThreshold() 
                    || deviationFromMean > this.config.getSuddenSpikeStdDeviationMultiplier())
                    && absoluteChange > this.config.getSuddenSpikeMinAbsoluteChange();
            
            if (isSuddenSpike) {
                // 计算置信度 - 取百分比变化和标准差倍数的最大比例
                double confidenceScore = Math.max(
                    percentageChange / this.config.getSuddenSpikePercentageChangeThreshold(),
                    deviationFromMean / this.config.getSuddenSpikeStdDeviationMultiplier()
                );
                
                // 限制置信度在0-1范围内
                confidenceScore = Math.min(1.0, confidenceScore);
                
                StringBuilder description = new StringBuilder();
                description.append(String.format("单日上涨%.2f%%，较前期平均值偏离%.2f个标准差", 
                        percentageChange, deviationFromMean));
                
                return AlertReport.builder()
                        .date(latestDate)
                        .isAlert(true)
                        .alertType(AlertType.SINGLE_DAY_SPIKE)
                        .totalScore(confidenceScore)
                        .description(description.toString())
                        .build();
            }
        }
        
        return createNormalReport(latestDate, "未检测到单日暴涨");
    }

    /**
     * 检测周期性波动
     */
    private boolean hasPeriodicity(double[] values) {
        // 数据不足时无法检测周期性
        if (values.length < this.config.getPeriodicityMaxPeriodDays() * 2) {
            log.debug("数据不足以检测周期性: {} < {}", values.length, this.config.getPeriodicityMaxPeriodDays() * 2);
            return false;
        }
        
        // 如果最后一个值有大幅波动，不考虑周期性
        if (values.length > 1) {
            double lastValue = values[values.length - 1];
            double previousValue = values[values.length - 2];
            double percentageChange = previousValue != 0 ? Math.abs((lastValue - previousValue) / previousValue * 100) : 0;
            
            // 如果最后一天变化超过阈值的一半，优先考虑为异常而非周期性
            if (percentageChange > this.config.getSuddenSpikePercentageChangeThreshold() / 2) {
                log.debug("最后一天变化过大，不考虑周期性: {}%", percentageChange);
                return false;
            }
        }
        
        // 计算标准差 - 如果波动很小，不考虑周期性
        DescriptiveStatistics stats = new DescriptiveStatistics(values);
        double stdDev = stats.getStandardDeviation();
        double mean = stats.getMean();
        double cv = mean != 0 ? stdDev / mean : 0; // 变异系数
        
        log.debug("数据变异系数: {}", cv);
        
        // 如果变异系数太小，说明数据波动不大，不考虑为周期性
        if (cv < 0.05) {
            log.debug("变异系数太小，不考虑周期性: {}", cv);
            return false;
        }
        
        // 计算自相关系数
        for (int lag = 1; lag <= Math.min(this.config.getPeriodicityMaxPeriodDays(), values.length / 3); lag++) {
            double[] series1 = new double[values.length - lag];
            double[] series2 = new double[values.length - lag];
            
            // 创建滞后序列
            for (int i = 0; i < values.length - lag; i++) {
                series1[i] = values[i];
                series2[i] = values[i + lag];
            }
            
            // 计算两个序列的相关系数
            double correlation = StatisticsUtil.correlation(series1, series2);
            log.debug("周期 {} 天的相关系数: {}", lag, correlation);
            
            // 如果相关系数大于阈值，认为存在周期性
            if (Math.abs(correlation) > this.config.getPeriodicityAutocorrelationThreshold()) {
                log.debug("检测到周期性波动，周期: {} 天, 相关系数: {}", lag, correlation);
                return true;
            }
        }
        
        log.debug("未检测到周期性波动");
        return false;
    }

    /**
     * 创建周期性波动结果
     */
    private AlertReport createPeriodicReport(LocalDate date, double[] values) {
        // 寻找最佳周期
        int bestPeriod = 0;
        double maxCorrelation = 0;
        
        // 检查可能的周期
        for (int lag = 1; lag <= Math.min(this.config.getPeriodicityMaxPeriodDays(), values.length / 3); lag++) {
            double[] series1 = new double[values.length - lag];
            double[] series2 = new double[values.length - lag];
            
            for (int i = 0; i < values.length - lag; i++) {
                series1[i] = values[i];
                series2[i] = values[i + lag];
            }
            
            double correlation = StatisticsUtil.correlation(series1, series2);
            
            if (Math.abs(correlation) > Math.abs(maxCorrelation)) {
                maxCorrelation = correlation;
                bestPeriod = lag;
            }
        }
        
        // 使用相关系数的绝对值作为置信度
        double confidenceScore = Math.abs(maxCorrelation);
        
        StringBuilder description = new StringBuilder();
        description.append(String.format("检测到周期性波动，周期约为%d天，相关系数为%.2f", 
                bestPeriod, maxCorrelation));
        
        // 计算基本分数
        double baseScore = confidenceScore;
        
        // 计算总分
        double totalScore = baseScore * this.config.getScorePeriodicWeight();
        
        // 确定严重性级别
        SeverityLevel severityLevel;
        if (totalScore >= this.config.getScoreCriticalThreshold()) {
            severityLevel = SeverityLevel.CRITICAL;
        } else if (totalScore >= this.config.getScoreWarningThreshold()) {
            severityLevel = SeverityLevel.WARNING;
        } else {
            severityLevel = SeverityLevel.NORMAL;
        }
        
        // 返回已经完全配置好的报告，避免后续处理修改类型
        return AlertReport.builder()
                .date(date)
                .isAlert(true)
                .alertType(AlertType.ABNORMAL_VOLATILITY)
                .totalScore(totalScore)
                .severityLevel(severityLevel)
                .description(description.toString())
                .build();
    }

    /**
     * 创建正常结果
     */
    private AlertReport createNormalReport(LocalDate date, String reason) {
        return AlertReport.builder()
                .date(date)
                .isAlert(false)
                .alertType(AlertType.NO_ISSUE)
                .totalScore(0.0)
                .severityLevel(SeverityLevel.NORMAL)
                .description(reason)
                .build();
    }

    /**
     * Builder类，用于链式构建异常检测服务
     */
    public static class Builder {
        private AnomalyDetectionConfig config;
        private Integer windowSize;
        
        public Builder() {
            // 默认使用null，会在构建时使用默认值
        }
        
        /**
         * 设置配置
         */
        public Builder withConfig(AnomalyDetectionConfig config) {
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
        public AnomalyDetectionWindowServiceImpl build() {
            if (windowSize != null && config != null) {
                return new AnomalyDetectionWindowServiceImpl(config, windowSize);
            } else if (config != null) {
                return new AnomalyDetectionWindowServiceImpl(config);
            } else {
                return new AnomalyDetectionWindowServiceImpl();
            }
        }
    }
    
    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
} 