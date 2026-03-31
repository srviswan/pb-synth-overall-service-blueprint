package com.pbsynth.tradecapture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradecapture.rules")
public class RulesProperties {
    private String resource = "classpath:rules-config.yml";

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }
}
