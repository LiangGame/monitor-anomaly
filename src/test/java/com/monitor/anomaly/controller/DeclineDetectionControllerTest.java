package com.monitor.anomaly.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.anomaly.config.DeclineDetectionConfig;
import com.monitor.anomaly.model.DataPointDTO;
import com.monitor.anomaly.model.AlertReport;
import com.monitor.anomaly.model.AlertType;
import com.monitor.anomaly.model.BatchDetectionRequest;
import com.monitor.anomaly.service.DeclineDetectionService;

/**
 * 下跌检测控制器测试类
 */
@WebMvcTest(DeclineDetectionController.class)
public class DeclineDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeclineDetectionService declineDetectionService;

    @Autowired
    private ObjectMapper objectMapper;

    private AlertReport normalReport;
    private AlertReport dropReport;
    private List<Double> values;
    private List<DataPointDTO> dataPoints;
    private DeclineDetectionConfig customConfig;

    @BeforeEach
    public void setup() {
        // 创建正常报告
        normalReport = AlertReport.builder()
                .alertType(AlertType.NO_ISSUE)
                .date(LocalDate.now())
                .totalScore(0.0)
                .description("未检测到异常")
                .isAlert(false)
                .build();

        // 创建下跌报告
        dropReport = AlertReport.builder()
                .alertType(AlertType.SINGLE_DAY_DROP)
                .date(LocalDate.now())
                .totalScore(1.5)
                .description("检测到单日暴跌异常，变化百分比: -15.0%")
                .isAlert(true)
                .build();

        // 创建值列表
        values = Arrays.asList(100.0, 95.0, 90.0, 85.0, 80.0, 75.0, 70.0);

        // 创建自定义配置
        customConfig = new DeclineDetectionConfig();
        customConfig.setSuddenDropChangePercentThreshold(5.0);
    }

    /**
     * 测试使用值列表批量检测
     */
    @Test
    public void testBatchDetectWithValues() throws Exception {
        // 设置Mock行为
        when(declineDetectionService.detectWithValues(anyList())).thenReturn(dropReport);

        // 执行请求并验证
        mockMvc.perform(post("/api/decline/batch-values")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(values)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertType").value("SINGLE_DAY_DROP"))
                .andExpect(jsonPath("$.isAlert").value(true))
                .andExpect(jsonPath("$.totalScore").value(1.5));
    }

    /**
     * 测试使用值列表和自定义配置批量检测
     */
    @Test
    public void testBatchDetectWithValuesAndConfig() throws Exception {
        // 创建请求对象
        BatchDetectionRequest<Double, DeclineDetectionConfig> request = new BatchDetectionRequest<>();
        request.setDataPoints(values);
        request.setConfig(customConfig);

        // 设置Mock行为
        when(declineDetectionService.detectWithValues(anyList(), any(DeclineDetectionConfig.class)))
                .thenReturn(dropReport);

        // 执行请求并验证
        mockMvc.perform(post("/api/decline/batch-values-with-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertType").value("SINGLE_DAY_DROP"))
                .andExpect(jsonPath("$.isAlert").value(true));
    }

    /**
     * 测试处理空数据
     */
    @Test
    public void testHandleEmptyData() throws Exception {
        // 设置Mock行为
        when(declineDetectionService.detectWithValues(anyList())).thenReturn(normalReport);

        // 执行请求并验证
        mockMvc.perform(post("/api/decline/batch-values")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertType").value("NO_ISSUE"))
                .andExpect(jsonPath("$.isAlert").value(false));
    }
} 