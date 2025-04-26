package com.monitor.anomaly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 异常检测配置类，整合了所有检测算法的配置参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "anomaly.detection")
public class AnomalyDetectionConfig {
    // ---------- 渐变上涨检测配置 ----------
    // 线性回归斜率阈值，大于该值视为渐变上涨
    private double gradualIncreaseSlopeThreshold = 0.25;
    
    // 要求的最小决定系数R²，衡量线性模型拟合程度
    private double gradualIncreaseMinRSquared = 0.6;
    
    // 最小连续上升天数
    private int gradualIncreaseMinConsecutiveIncreases = 3;
    
    // 累计涨幅阈值（%），总体涨幅超过该值才会触发警报
    private double gradualIncreaseTotalChangePercentThreshold = 100.0;
    
    // ---------- 暴涨检测配置 ----------
    // 相对于前一天的突增百分比阈值
    private double suddenSpikePercentageChangeThreshold = 100.0;
    
    // 相对于历史平均值的标准差倍数，超过视为暴涨
    private double suddenSpikeStdDeviationMultiplier = 3.0;
    
    // 最小绝对变化值，避免基数小时误判
    private double suddenSpikeMinAbsoluteChange = 10.0;
    
    // ---------- 周期性波动检测配置 ----------
    // 自相关系数阈值，超过该值视为具有周期性
    private double periodicityAutocorrelationThreshold = 0.7;
    
    // 检测的最大周期天数
    private int periodicityMaxPeriodDays = 7;
    
    // ---------- 评分配置 ----------
    // 暴涨权重 - 高优先级
    private double scoreSuddenSpikeWeight = 10.0;
    
    // 渐变上涨权重 - 中优先级
    private double scoreGradualIncreaseWeight = 5.0;
    
    // 周期性波动权重 - 低优先级
    private double scorePeriodicWeight = 1.0;
    
    // 严重异常阈值
    private double scoreCriticalThreshold = 7.5;
    
    // 警告阈值
    private double scoreWarningThreshold = 5.0;
    
    /**
     * 创建配置的副本
     * @return 配置副本
     */
    public AnomalyDetectionConfig copy() {
        AnomalyDetectionConfig copy = new AnomalyDetectionConfig();
        
        // 复制渐变上涨配置
        copy.setGradualIncreaseSlopeThreshold(this.gradualIncreaseSlopeThreshold);
        copy.setGradualIncreaseMinRSquared(this.gradualIncreaseMinRSquared);
        copy.setGradualIncreaseMinConsecutiveIncreases(this.gradualIncreaseMinConsecutiveIncreases);
        copy.setGradualIncreaseTotalChangePercentThreshold(this.gradualIncreaseTotalChangePercentThreshold);
        
        // 复制暴涨配置
        copy.setSuddenSpikePercentageChangeThreshold(this.suddenSpikePercentageChangeThreshold);
        copy.setSuddenSpikeStdDeviationMultiplier(this.suddenSpikeStdDeviationMultiplier);
        copy.setSuddenSpikeMinAbsoluteChange(this.suddenSpikeMinAbsoluteChange);
        
        // 复制周期性配置
        copy.setPeriodicityAutocorrelationThreshold(this.periodicityAutocorrelationThreshold);
        copy.setPeriodicityMaxPeriodDays(this.periodicityMaxPeriodDays);
        
        // 复制评分配置
        copy.setScoreSuddenSpikeWeight(this.scoreSuddenSpikeWeight);
        copy.setScoreGradualIncreaseWeight(this.scoreGradualIncreaseWeight);
        copy.setScorePeriodicWeight(this.scorePeriodicWeight);
        copy.setScoreCriticalThreshold(this.scoreCriticalThreshold);
        copy.setScoreWarningThreshold(this.scoreWarningThreshold);
        
        return copy;
    }
    
    /**
     * 将传入的自定义配置与当前配置合并
     * 只合并非null的值
     * @param customConfig 自定义配置
     * @return 合并后的配置
     */
    public AnomalyDetectionConfig merge(AnomalyDetectionConfig customConfig) {
        if (customConfig == null) {
            return this.copy();
        }
        
        AnomalyDetectionConfig merged = this.copy();
        
        // 渐变上涨配置合并
        if (customConfig.getGradualIncreaseSlopeThreshold() > 0) {
            merged.setGradualIncreaseSlopeThreshold(customConfig.getGradualIncreaseSlopeThreshold());
        }
        if (customConfig.getGradualIncreaseMinRSquared() > 0) {
            merged.setGradualIncreaseMinRSquared(customConfig.getGradualIncreaseMinRSquared());
        }
        if (customConfig.getGradualIncreaseMinConsecutiveIncreases() > 0) {
            merged.setGradualIncreaseMinConsecutiveIncreases(customConfig.getGradualIncreaseMinConsecutiveIncreases());
        }
        if (customConfig.getGradualIncreaseTotalChangePercentThreshold() > 0) {
            merged.setGradualIncreaseTotalChangePercentThreshold(customConfig.getGradualIncreaseTotalChangePercentThreshold());
        }
        
        // 暴涨配置合并
        if (customConfig.getSuddenSpikePercentageChangeThreshold() > 0) {
            merged.setSuddenSpikePercentageChangeThreshold(customConfig.getSuddenSpikePercentageChangeThreshold());
        }
        if (customConfig.getSuddenSpikeStdDeviationMultiplier() > 0) {
            merged.setSuddenSpikeStdDeviationMultiplier(customConfig.getSuddenSpikeStdDeviationMultiplier());
        }
        if (customConfig.getSuddenSpikeMinAbsoluteChange() > 0) {
            merged.setSuddenSpikeMinAbsoluteChange(customConfig.getSuddenSpikeMinAbsoluteChange());
        }
        
        // 周期性配置合并
        if (customConfig.getPeriodicityAutocorrelationThreshold() > 0) {
            merged.setPeriodicityAutocorrelationThreshold(customConfig.getPeriodicityAutocorrelationThreshold());
        }
        if (customConfig.getPeriodicityMaxPeriodDays() > 0) {
            merged.setPeriodicityMaxPeriodDays(customConfig.getPeriodicityMaxPeriodDays());
        }
        
        // 评分配置合并
        if (customConfig.getScoreSuddenSpikeWeight() > 0) {
            merged.setScoreSuddenSpikeWeight(customConfig.getScoreSuddenSpikeWeight());
        }
        if (customConfig.getScoreGradualIncreaseWeight() > 0) {
            merged.setScoreGradualIncreaseWeight(customConfig.getScoreGradualIncreaseWeight());
        }
        if (customConfig.getScorePeriodicWeight() > 0) {
            merged.setScorePeriodicWeight(customConfig.getScorePeriodicWeight());
        }
        if (customConfig.getScoreCriticalThreshold() > 0) {
            merged.setScoreCriticalThreshold(customConfig.getScoreCriticalThreshold());
        }
        if (customConfig.getScoreWarningThreshold() > 0) {
            merged.setScoreWarningThreshold(customConfig.getScoreWarningThreshold());
        }
        
        return merged;
    }
} 