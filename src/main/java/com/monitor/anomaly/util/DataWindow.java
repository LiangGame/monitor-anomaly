package com.monitor.anomaly.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;

/**
 * 滑动窗口数据结构，用于存储和处理固定时间窗口内的指标数据
 */
public class DataWindow {
    
    private final LinkedList<DataPoint> dataPoints;
    
    // 默认短期移动平均天数
    private static final int DEFAULT_SHORT_TERM_DAYS = 3;
    
    // 默认长期移动平均天数
    private static final int DEFAULT_LONG_TERM_DAYS = 7;
    
    // 默认窗口大小
    private static final int DEFAULT_MAX_SIZE = 7;
    
    // 短期移动平均天数
    private int shortTermDays;
    
    // 长期移动平均天数
    private int longTermDays;
    
    // 窗口最大容量
    private int maxSize;
    
    /**
     * 创建默认数据窗口
     */
    public DataWindow() {
        this(DEFAULT_MAX_SIZE);
    }
    
    /**
     * 创建指定大小的数据窗口
     * 
     * @param maxSize 窗口最大容量（天数）
     */
    public DataWindow(int maxSize) {
        this(maxSize, DEFAULT_SHORT_TERM_DAYS, DEFAULT_LONG_TERM_DAYS);
    }
    
    /**
     * 创建数据窗口，指定短期和长期移动平均天数
     * 
     * @param maxSize 窗口最大容量（天数）
     * @param shortTermDays 短期移动平均天数
     * @param longTermDays 长期移动平均天数
     */
    public DataWindow(int maxSize, int shortTermDays, int longTermDays) {
        this.maxSize = maxSize > 0 ? maxSize : DEFAULT_MAX_SIZE;
        this.dataPoints = new LinkedList<>();
        this.shortTermDays = shortTermDays;
        this.longTermDays = longTermDays;
    }
    
    /**
     * 添加数据点到窗口尾部
     * 如果窗口已满，则移除最旧的数据点
     * 添加时计算环比值和移动平均
     * 
     * @param date 日期
     * @param value 值
     */
    public void addDataPoint(LocalDate date, double value) {
        if (dataPoints.size() >= maxSize) {
            dataPoints.removeFirst();
        }
        
        // 创建新数据点
        DataPoint newPoint = new DataPoint(date, value);
        
        // 计算环比指标和移动平均
        calculateMetrics(newPoint);
        
        dataPoints.addLast(newPoint);
        
        // 确保按日期排序
        Collections.sort(dataPoints);
    }
    
    /**
     * 计算数据点的环比指标和移动平均
     * 
     * @param newPoint 新数据点
     */
    private void calculateMetrics(DataPoint newPoint) {
        LocalDate date = newPoint.getDate();
        double value = newPoint.getValue();
        
        // 查找前一天的数据点计算环比
        DataPoint yesterdayPoint = findDataPointByDate(date.minusDays(1));
        if (yesterdayPoint != null) {
            double yesterdayValue = yesterdayPoint.getValue();
            
            // 计算环比差值
            double chainDiff = value - yesterdayValue;
            newPoint.setChainDiff(chainDiff);
            
            // 计算环比率
            double chainRatio = yesterdayValue != 0 ? value / yesterdayValue : 0;
            newPoint.setChainRatio(chainRatio);
        }
        
        // 计算短期移动平均
        double maShortSum = 0;
        int maShortCount = 0;
        
        // 查找最近短期天数的所有点
        List<DataPoint> recentPoints = dataPoints.stream()
                .filter(p -> p.getDate().isBefore(date))
                .sorted((p1, p2) -> p2.getDate().compareTo(p1.getDate())) // 按日期降序
                .limit(shortTermDays - 1)
                .collect(Collectors.toList());
        
        // 先加入当前值
        maShortSum += value;
        maShortCount++;
        
        // 加入历史数据
        for (DataPoint point : recentPoints) {
            maShortSum += point.getValue();
            maShortCount++;
        }
        
        // 计算短期移动平均值
        double maShort = maShortCount > 0 ? maShortSum / maShortCount : value;
        newPoint.setMaShort(maShort);
        
        // 计算长期移动平均
        double maLongSum = maShortSum; // 已经计算了一部分
        int maLongCount = maShortCount;
        
        // 查找额外的长期数据点
        if (longTermDays > shortTermDays) {
            List<DataPoint> additionalPoints = dataPoints.stream()
                    .filter(p -> p.getDate().isBefore(date))
                    .sorted((p1, p2) -> p2.getDate().compareTo(p1.getDate())) // 按日期降序
                    .skip(shortTermDays - 1) // 跳过已计算的点
                    .limit(longTermDays - shortTermDays)
                    .collect(Collectors.toList());
            
            for (DataPoint point : additionalPoints) {
                maLongSum += point.getValue();
                maLongCount++;
            }
        }
        
        // 计算长期移动平均值
        double maLong = maLongCount > 0 ? maLongSum / maLongCount : value;
        newPoint.setMaLong(maLong);
    }
    
    /**
     * 通过日期查找数据点
     */
    private DataPoint findDataPointByDate(LocalDate date) {
        return dataPoints.stream()
                .filter(p -> p.getDate().equals(date))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 批量添加数据点
     * 
     * @param dates 日期数组
     * @param values 值数组
     */
    public void addDataPoints(LocalDate[] dates, double[] values) {
        if (dates.length != values.length) {
            throw new IllegalArgumentException("日期和值数组长度必须一致");
        }
        
        for (int i = 0; i < dates.length; i++) {
            addDataPoint(dates[i], values[i]);
        }
    }
    
    /**
     * 获取窗口内所有值，按日期排序
     */
    public double[] getValues() {
        return dataPoints.stream()
                .mapToDouble(DataPoint::getValue)
                .toArray();
    }
    
    /**
     * 获取窗口内所有日期，按日期排序
     */
    public LocalDate[] getDates() {
        return dataPoints.stream()
                .map(DataPoint::getDate)
                .toArray(LocalDate[]::new);
    }
    
    /**
     * 获取窗口内所有数据点
     */
    public List<DataPoint> getDataPoints() {
        return new ArrayList<>(dataPoints);
    }
    
    /**
     * 获取窗口大小
     */
    public int size() {
        return dataPoints.size();
    }
    
    /**
     * 获取窗口最大容量
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * 设置窗口最大容量
     */
    public void setMaxSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("窗口最大容量必须大于0");
        }
        this.maxSize = maxSize;
        
        // 如果当前数据点超过新的最大容量，移除多余的数据点
        while (dataPoints.size() > maxSize) {
            dataPoints.removeFirst();
        }
    }
    
    /**
     * 检查窗口是否为空
     */
    public boolean isEmpty() {
        return dataPoints.isEmpty();
    }
    
    /**
     * 检查窗口是否已满
     */
    public boolean isFull() {
        return dataPoints.size() >= maxSize;
    }
    
    /**
     * 清空窗口
     */
    public void clear() {
        dataPoints.clear();
    }
    
    /**
     * 滑动窗口，移除最旧的数据点，添加新数据点
     * 
     * @param date 新数据点日期
     * @param value 新数据点值
     * @return 被移除的数据点
     */
    public DataPoint slide(LocalDate date, double value) {
        DataPoint removed = null;
        if (!dataPoints.isEmpty()) {
            removed = dataPoints.removeFirst();
        }
        
        // 添加新数据点，会自动计算指标
        addDataPoint(date, value);
        
        return removed;
    }
    
    /**
     * 获取指定天数的子窗口
     * 
     * @param days 天数
     * @return 子窗口
     */
    public DataWindow getSubWindow(int days) {
        if (days > dataPoints.size()) {
            days = dataPoints.size();
        }
        
        DataWindow subWindow = new DataWindow(days, shortTermDays, longTermDays);
        
        List<DataPoint> subPoints = dataPoints.stream()
                .sorted()
                .skip(dataPoints.size() - days)
                .collect(Collectors.toList());
        
        for (DataPoint point : subPoints) {
            // 创建带有完整指标的数据点
            DataPoint newPoint = new DataPoint(point.getDate(), point.getValue());
            newPoint.setChainDiff(point.getChainDiff());
            newPoint.setChainRatio(point.getChainRatio());
            newPoint.setMaShort(point.getMaShort());
            newPoint.setMaLong(point.getMaLong());
            
            // 直接添加到队列，避免重复计算
            subWindow.dataPoints.addLast(newPoint);
        }
        
        return subWindow;
    }
    
    /**
     * 设置短期移动平均天数
     * 
     * @param days 天数
     */
    public void setShortTermDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("短期移动平均天数必须大于0");
        }
        this.shortTermDays = days;
        // 重新计算所有数据点的短期移动平均
        recalculateMetrics();
    }
    
    /**
     * 设置长期移动平均天数
     * 
     * @param days 天数
     */
    public void setLongTermDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("长期移动平均天数必须大于0");
        }
        this.longTermDays = days;
        // 重新计算所有数据点的长期移动平均
        recalculateMetrics();
    }
    
    /**
     * 重新计算所有数据点的指标
     */
    private void recalculateMetrics() {
        if (dataPoints.isEmpty()) {
            return;
        }
        
        // 先按日期排序
        Collections.sort(dataPoints);
        
        // 重新计算环比差值和环比率
        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint point = dataPoints.get(i);
            
            // 清除之前的计算结果
            point.setChainDiff(0);
            point.setChainRatio(0);
            
            // 如果不是第一个点，计算环比
            if (i > 0) {
                DataPoint previousPoint = dataPoints.get(i - 1);
                double value = point.getValue();
                double previousValue = previousPoint.getValue();
                
                // 计算环比差值
                double chainDiff = value - previousValue;
                point.setChainDiff(chainDiff);
                
                // 计算环比率
                double chainRatio = previousValue != 0 ? value / previousValue : 0;
                point.setChainRatio(chainRatio);
            }
        }
        
        // 重新计算移动平均
        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint point = dataPoints.get(i);
            
            // 计算短期移动平均
            double maShortSum = 0;
            int maShortCount = 0;
            
            for (int j = Math.max(0, i - shortTermDays + 1); j <= i; j++) {
                maShortSum += dataPoints.get(j).getValue();
                maShortCount++;
            }
            
            double maShort = maShortCount > 0 ? maShortSum / maShortCount : point.getValue();
            point.setMaShort(maShort);
            
            // 计算长期移动平均
            double maLongSum = 0;
            int maLongCount = 0;
            
            for (int j = Math.max(0, i - longTermDays + 1); j <= i; j++) {
                maLongSum += dataPoints.get(j).getValue();
                maLongCount++;
            }
            
            double maLong = maLongCount > 0 ? maLongSum / maLongCount : point.getValue();
            point.setMaLong(maLong);
        }
    }
    
    /**
     * 数据点类，包含日期和值，以及衍生指标
     */
    public static class DataPoint implements Comparable<DataPoint> {
        private final LocalDate date;
        private final double value;
        
        // 环比差值（与前一天相比的差值）
        private double chainDiff;
        
        // 环比率（与前一天相比的比率）
        private double chainRatio;
        
        // 短期移动平均
        private double maShort;
        
        // 长期移动平均
        private double maLong;
        
        public DataPoint(LocalDate date, double value) {
            this.date = date;
            this.value = value;
            this.chainDiff = 0;
            this.chainRatio = 0;
            this.maShort = 0;
            this.maLong = 0;
        }
        
        public LocalDate getDate() {
            return date;
        }
        
        public double getValue() {
            return value;
        }
        
        public double getChainDiff() {
            return chainDiff;
        }
        
        public void setChainDiff(double chainDiff) {
            this.chainDiff = chainDiff;
        }
        
        public double getChainRatio() {
            return chainRatio;
        }
        
        public void setChainRatio(double chainRatio) {
            this.chainRatio = chainRatio;
        }
        
        public double getMaShort() {
            return maShort;
        }
        
        public void setMaShort(double maShort) {
            this.maShort = maShort;
        }
        
        public double getMaLong() {
            return maLong;
        }
        
        public void setMaLong(double maLong) {
            this.maLong = maLong;
        }
        
        @Override
        public int compareTo(DataPoint other) {
            return this.date.compareTo(other.date);
        }
    }
    
    /**
     * 将数据窗口转换为JSONArray字符串
     * 使用FastJSON序列化，包含所有数据点详细信息
     * 
     * @return JSONArray格式的字符串
     */
    public String toJsonArray() {
        if (dataPoints.isEmpty()) {
            return "[]";
        }
        return JSON.toJSONString(dataPoints, SerializerFeature.WriteMapNullValue);
    }
    
    /**
     * 将数据窗口转换为JSONArray字符串，只包含日期和值
     * 使用FastJSON，适用于需要简化输出的场景
     * 
     * @return 简化的JSONArray格式字符串
     */
    public String toSimpleJsonArray() {
        if (dataPoints.isEmpty()) {
            return "[]";
        }
        
        JSONArray jsonArray = new JSONArray();
        for (DataPoint point : dataPoints) {
            // 创建只包含日期和值的简化对象
            SimplifiedDataPoint simplePoint = new SimplifiedDataPoint(
                point.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                point.getValue()
            );
            jsonArray.add(simplePoint);
        }
        
        return jsonArray.toJSONString();
    }
    
    /**
     * 简化的数据点类，仅包含日期和值
     * 用于生成简化版JSON
     */
    private static class SimplifiedDataPoint {
        private String date;
        private double value;
        
        public SimplifiedDataPoint(String date, double value) {
            this.date = date;
            this.value = value;
        }
        
        public String getDate() {
            return date;
        }
        
        public double getValue() {
            return value;
        }
    }
} 