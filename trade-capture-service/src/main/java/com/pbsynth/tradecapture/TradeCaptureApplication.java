package com.pbsynth.tradecapture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.pbsynth.tradecapture.config.DispatchProperties;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableConfigurationProperties(DispatchProperties.class)
public class TradeCaptureApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeCaptureApplication.class, args);
    }
}
