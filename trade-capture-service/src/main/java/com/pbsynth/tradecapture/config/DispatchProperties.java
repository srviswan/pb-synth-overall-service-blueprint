package com.pbsynth.tradecapture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradecapture.dispatch")
public class DispatchProperties {
    private String lifecycleBaseUrl;
    private int batchSize = 100;
    private int maxAttempts = 5;

    public String getLifecycleBaseUrl() {
        return lifecycleBaseUrl;
    }

    public void setLifecycleBaseUrl(String lifecycleBaseUrl) {
        this.lifecycleBaseUrl = lifecycleBaseUrl;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
