package com.monitor.anomaly.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据点DTO，用于API调用时传递数据点信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPointDTO {
    /**
     * 数据点日期
     */
    private Date date;
    
    /**
     * 数据点值
     */
    private Double value;
} 