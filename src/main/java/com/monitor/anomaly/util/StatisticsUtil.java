package com.monitor.anomaly.util;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * 统计工具类，替代Apache Commons Math3库，实现必要的统计学计算
 */
public class StatisticsUtil {

    /**
     * 计算均值
     */
    public static double mean(double[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }
    
    /**
     * 计算List<Double>均值
     */
    public static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * 计算标准差
     */
    public static double standardDeviation(double[] values) {
        double mean = mean(values);
        double sum = Arrays.stream(values)
                .map(x -> Math.pow(x - mean, 2))
                .sum();
        return Math.sqrt(sum / (values.length > 1 ? values.length - 1 : 1));
    }
    
    /**
     * 计算List<Double>标准差
     */
    public static double standardDeviation(List<Double> values) {
        double mean = mean(values);
        double sum = values.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .sum();
        return Math.sqrt(sum / (values.size() > 1 ? values.size() - 1 : 1));
    }

    /**
     * 计算Pearson相关系数
     */
    public static double correlation(double[] x, double[] y) {
        if (x.length != y.length || x.length == 0) {
            return 0.0;
        }

        double meanX = mean(x);
        double meanY = mean(y);
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;

        for (int i = 0; i < x.length; i++) {
            double xDiff = x[i] - meanX;
            double yDiff = y[i] - meanY;
            sumXY += xDiff * yDiff;
            sumX2 += xDiff * xDiff;
            sumY2 += yDiff * yDiff;
        }

        if (sumX2 == 0.0 || sumY2 == 0.0) {
            return 0.0;
        }
        
        return sumXY / (Math.sqrt(sumX2) * Math.sqrt(sumY2));
    }

    /**
     * 创建线性回归对象
     * 
     * @param x X坐标数组
     * @param y Y坐标数组
     * @return 线性回归对象
     */
    public static LinearRegression linearRegression(double[] x, double[] y) {
        LinearRegression regression = new LinearRegression();
        regression.addData(x, y);
        return regression;
    }
    
    /**
     * 线性回归计算类
     */
    public static class LinearRegression {
        private double slope;
        private double intercept;
        private double rSquare;
        private boolean calculated = false;

        public void addData(double[][] data) {
            for (double[] point : data) {
                addData(point[0], point[1]);
            }
        }

        public void addData(double x, double y) {
            // 添加单个数据点
            double[] newX = Arrays.copyOf(xValues, xValues.length + 1);
            double[] newY = Arrays.copyOf(yValues, yValues.length + 1);
            
            newX[xValues.length] = x;
            newY[yValues.length] = y;
            
            xValues = newX;
            yValues = newY;
            
            calculated = false;
        }

        private double[] xValues = new double[0];
        private double[] yValues = new double[0];

        public void addData(double[] x, double[] y) {
            if (x.length != y.length) {
                throw new IllegalArgumentException("Array lengths must match");
            }
            
            // 合并新旧数据
            int oldLength = xValues.length;
            int newLength = oldLength + x.length;
            
            double[] newX = Arrays.copyOf(xValues, newLength);
            double[] newY = Arrays.copyOf(yValues, newLength);
            
            System.arraycopy(x, 0, newX, oldLength, x.length);
            System.arraycopy(y, 0, newY, oldLength, y.length);
            
            xValues = newX;
            yValues = newY;
            
            calculated = false;
        }

        private void calculate() {
            if (calculated || xValues.length < 2) {
                return;
            }

            double n = xValues.length;
            double sumX = Arrays.stream(xValues).sum();
            double sumY = Arrays.stream(yValues).sum();
            double sumXY = 0.0;
            double sumX2 = 0.0;
            double sumY2 = 0.0;

            for (int i = 0; i < n; i++) {
                sumXY += xValues[i] * yValues[i];
                sumX2 += xValues[i] * xValues[i];
                sumY2 += yValues[i] * yValues[i];
            }

            // 计算斜率和截距
            double xMean = sumX / n;
            double yMean = sumY / n;
            
            if (sumX2 - (sumX * sumX) / n == 0) {
                slope = 0;
                intercept = yMean;
            } else {
                slope = (sumXY - (sumX * sumY) / n) / (sumX2 - (sumX * sumX) / n);
                intercept = yMean - slope * xMean;
            }

            // 计算R^2
            double totalSS = sumY2 - (sumY * sumY) / n;
            double residualSS = 0.0;
            for (int i = 0; i < n; i++) {
                double prediction = intercept + slope * xValues[i];
                residualSS += Math.pow(yValues[i] - prediction, 2);
            }
            
            if (totalSS == 0) {
                rSquare = 1.0; // 完美拟合
            } else {
                rSquare = 1 - (residualSS / totalSS);
            }

            calculated = true;
        }

        public double getSlope() {
            calculate();
            return slope;
        }

        public double getIntercept() {
            calculate();
            return intercept;
        }

        public double getRSquare() {
            calculate();
            return rSquare;
        }
        
        /**
         * 获取确定系数，与getRSquare()相同
         */
        public double getRSquared() {
            return getRSquare();
        }
    }

    /**
     * 描述性统计类
     */
    public static class DescriptiveStatistics {
        private double[] values;

        public DescriptiveStatistics(double[] values) {
            this.values = values.clone();
        }

        public double getMean() {
            return mean(values);
        }

        public double getStandardDeviation() {
            return standardDeviation(values);
        }

        public double getMin() {
            return Arrays.stream(values).min().orElse(0.0);
        }

        public double getMax() {
            return Arrays.stream(values).max().orElse(0.0);
        }

        public DoubleSummaryStatistics getSummary() {
            return Arrays.stream(values).summaryStatistics();
        }
    }
} 