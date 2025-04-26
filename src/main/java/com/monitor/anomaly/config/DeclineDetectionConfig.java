package com.monitor.anomaly.config;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下跌检测配置类
 */
@Data
@NoArgsConstructor
public class DeclineDetectionConfig {

    /**
     * 突然暴跌 - 变化百分比阈值，超过该值视为暴跌
     * 例如：30.0 表示下跌超过30%触发告警
     */
    private double suddenDropChangePercentThreshold = 30.0;
    
    /**
     * 突然暴跌 - 权重，用于计算最终得分
     */
    private double suddenDropWeight = 0.8;
    
    /**
     * 突然暴跌 - 相对于历史平均值的标准差倍数
     * 例如：3.0 表示偏离均值3个标准差时触发告警
     */
    private double suddenDropStdDeviationMultiplier = 3.0;
    
    /**
     * 突然暴跌 - 最小绝对变化值
     * 避免基数小时误判
     */
    private double suddenDropMinAbsoluteChange = 10.0;
    
    /**
     * 持续下降 - R²确定系数阈值
     * 越接近1表示线性关系越强
     */
    private double steadyDeclineRSquaredThreshold = 0.6;
    
    /**
     * 持续下降 - 最小连续下降天数
     */
    private int steadyDeclineMinConsecutiveDays = 3;
    
    /**
     * 持续下降 - 总体变化百分比阈值
     * 例如：50.0 表示总体下降超过50%触发告警
     */
    private double steadyDeclineTotalChangeThreshold = 50.0;
    
    /**
     * 持续下降 - 日均下降百分比阈值
     * 例如：15.0 表示日均下降超过15%触发告警
     */
    private double steadyDailyAverageDeclineThreshold = 15.0;
    
    /**
     * 持续下降 - 最小数据点数量
     */
    private int steadyDeclineMinDataPoints = 5;
    
    /**
     * 持续下降 - 权重，用于计算最终得分
     */
    private double steadyDeclineWeight = 0.7;
    
    /**
     * 周期性波动 - 最小方向变化次数
     * 例如：2 表示至少需要2次方向变化（上升转下降、下降转上升）
     */
    private int periodicFluctuationMinDirectionChanges = 2;
    
    /**
     * 周期性波动 - 最小变化百分比阈值
     * 例如：5.0 表示每次变化至少5%才被视为有效变化
     */
    private double periodicFluctuationMinChangePercent = 5.0;
    
    /**
     * 周期性波动 - 权重，用于计算最终得分
     */
    private double periodicFluctuationWeight = 0.6;
    
    /**
     * 周期性波动 - 自相关系数阈值
     * 检测周期性的指标，越接近1表示周期性越强
     */
    private double periodicFluctuationAutocorrelationThreshold = 0.7;
    
    /**
     * 周期性波动 - 最大周期天数
     * 检测周期的最大长度
     */
    private int periodicFluctuationMaxPeriodDays = 7;
    
    /**
     * 评分配置 - 严重警报阈值
     */
    private double scoreCriticalThreshold = 7.5;
    
    /**
     * 评分配置 - 警告阈值
     */
    private double scoreWarningThreshold = 5.0;

    /**
     * 合并配置，优先使用传入的配置，为null则使用当前配置
     */
    public DeclineDetectionConfig merge(DeclineDetectionConfig other) {
        if (other == null) {
            return this;
        }
        
        DeclineDetectionConfig merged = new DeclineDetectionConfig();
        
        // 合并突然暴跌配置
        merged.setSuddenDropChangePercentThreshold(
                other.getSuddenDropChangePercentThreshold() > 0 
                ? other.getSuddenDropChangePercentThreshold() 
                : this.getSuddenDropChangePercentThreshold());
        merged.setSuddenDropWeight(
                other.getSuddenDropWeight() > 0 
                ? other.getSuddenDropWeight() 
                : this.getSuddenDropWeight());
        merged.setSuddenDropStdDeviationMultiplier(
                other.getSuddenDropStdDeviationMultiplier() > 0 
                ? other.getSuddenDropStdDeviationMultiplier() 
                : this.getSuddenDropStdDeviationMultiplier());
        merged.setSuddenDropMinAbsoluteChange(
                other.getSuddenDropMinAbsoluteChange() > 0 
                ? other.getSuddenDropMinAbsoluteChange() 
                : this.getSuddenDropMinAbsoluteChange());
        
        // 合并持续下降配置
        merged.setSteadyDeclineRSquaredThreshold(
                other.getSteadyDeclineRSquaredThreshold() > 0 
                ? other.getSteadyDeclineRSquaredThreshold() 
                : this.getSteadyDeclineRSquaredThreshold());
        merged.setSteadyDeclineMinConsecutiveDays(
                other.getSteadyDeclineMinConsecutiveDays() > 0 
                ? other.getSteadyDeclineMinConsecutiveDays() 
                : this.getSteadyDeclineMinConsecutiveDays());
        merged.setSteadyDeclineTotalChangeThreshold(
                other.getSteadyDeclineTotalChangeThreshold() > 0 
                ? other.getSteadyDeclineTotalChangeThreshold() 
                : this.getSteadyDeclineTotalChangeThreshold());
        merged.setSteadyDailyAverageDeclineThreshold(
                other.getSteadyDailyAverageDeclineThreshold() > 0 
                ? other.getSteadyDailyAverageDeclineThreshold() 
                : this.getSteadyDailyAverageDeclineThreshold());
        merged.setSteadyDeclineMinDataPoints(
                other.getSteadyDeclineMinDataPoints() > 0 
                ? other.getSteadyDeclineMinDataPoints() 
                : this.getSteadyDeclineMinDataPoints());
        merged.setSteadyDeclineWeight(
                other.getSteadyDeclineWeight() > 0 
                ? other.getSteadyDeclineWeight() 
                : this.getSteadyDeclineWeight());
        
        // 合并周期性波动配置
        merged.setPeriodicFluctuationMinDirectionChanges(
                other.getPeriodicFluctuationMinDirectionChanges() > 0 
                ? other.getPeriodicFluctuationMinDirectionChanges() 
                : this.getPeriodicFluctuationMinDirectionChanges());
        merged.setPeriodicFluctuationMinChangePercent(
                other.getPeriodicFluctuationMinChangePercent() > 0 
                ? other.getPeriodicFluctuationMinChangePercent() 
                : this.getPeriodicFluctuationMinChangePercent());
        merged.setPeriodicFluctuationWeight(
                other.getPeriodicFluctuationWeight() > 0 
                ? other.getPeriodicFluctuationWeight() 
                : this.getPeriodicFluctuationWeight());
        merged.setPeriodicFluctuationAutocorrelationThreshold(
                other.getPeriodicFluctuationAutocorrelationThreshold() > 0 
                ? other.getPeriodicFluctuationAutocorrelationThreshold() 
                : this.getPeriodicFluctuationAutocorrelationThreshold());
        merged.setPeriodicFluctuationMaxPeriodDays(
                other.getPeriodicFluctuationMaxPeriodDays() > 0 
                ? other.getPeriodicFluctuationMaxPeriodDays() 
                : this.getPeriodicFluctuationMaxPeriodDays());
        
        // 合并评分配置
        merged.setScoreCriticalThreshold(
                other.getScoreCriticalThreshold() > 0 
                ? other.getScoreCriticalThreshold() 
                : this.getScoreCriticalThreshold());
        merged.setScoreWarningThreshold(
                other.getScoreWarningThreshold() > 0 
                ? other.getScoreWarningThreshold() 
                : this.getScoreWarningThreshold());
        
        return merged;
    }
} 