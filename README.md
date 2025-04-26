# 异常检测系统 (Anomaly Detection System)

基于统计学方法的异常波动检测系统，用于监控业务指标的异常上涨模式。

## 支持的异常模式

1. **渐变上涨**：新版本灰度发布等场景下的缓慢上涨
   - 使用线性回归和连续上升天数检测
   - 配置斜率阈值、R²最小值和最小连续上升天数

2. **暴涨**：突发故障导致的断崖式上涨
   - 使用百分比变化、标准差倍数和绝对变化值检测
   - 配置百分比阈值、标准差倍数和最小绝对变化

3. **周期性波动识别**：避免将每周固定波动误判为异常
   - 使用自相关分析检测周期性模式
   - 配置自相关系数阈值和最大周期天数

## 主要特性

- 基于Spring Boot实现的REST API
- 支持通过配置文件自定义检测阈值
- 提供详细的异常分析结果和置信度
- 支持天维度的时间序列数据分析

## 技术栈

- **Java 11+**
- **Spring Boot 2.7.3**
- **Apache Commons Math 3.6.1**：用于统计计算
- **Lombok**：简化代码

## 使用方法

### API接口

POST `/api/anomaly/detect`

**请求体示例**:
```json
{
  "metricName": "api_error_rate",
  "dataPoints": [
    { "date": "2023-09-01", "value": 100 },
    { "date": "2023-09-02", "value": 105 },
    { "date": "2023-09-03", "value": 102 },
    { "date": "2023-09-04", "value": 108 },
    { "date": "2023-09-05", "value": 103 },
    { "date": "2023-09-06", "value": 110 },
    { "date": "2023-09-07", "value": 200 }
  ]
}
```

**响应示例**:
```json
{
  "metricName": "api_error_rate",
  "anomalyDetected": true,
  "anomalyType": "SUDDEN_SPIKE",
  "confidenceScore": 3.5,
  "details": {
    "percentageChange": 81.82,
    "absoluteChange": 90.0,
    "deviationFromMean": 3.2
  }
}
```

### 自定义阈值配置

可在`application.yml`中自定义阈值:

```yaml
anomaly:
  detection:
    gradual-increase:
      slope-threshold: 0.1
      min-r-squared: 0.7
      min-consecutive-increases: 3
    
    sudden-spike:
      percentage-change-threshold: 50.0
      std-deviation-multiplier: 2.5
      min-absolute-change: 10.0
    
    periodicity:
      autocorrelation-threshold: 0.7
      max-period-days: 7
```

## 启动应用

```bash
# 编译
mvn clean package

# 运行
java -jar target/anomaly-detection-0.0.1-SNAPSHOT.jar
```

应用默认运行在`http://localhost:8080` 