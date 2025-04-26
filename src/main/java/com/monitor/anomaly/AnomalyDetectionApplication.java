package com.monitor.anomaly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class AnomalyDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnomalyDetectionApplication.class, args);
    }
} 