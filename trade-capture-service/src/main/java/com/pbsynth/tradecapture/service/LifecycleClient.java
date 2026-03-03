package com.pbsynth.tradecapture.service;

import com.pbsynth.tradecapture.config.DispatchProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LifecycleClient {
    private final RestTemplate restTemplate;
    private final DispatchProperties dispatchProperties;

    public LifecycleClient(RestTemplate restTemplate, DispatchProperties dispatchProperties) {
        this.restTemplate = restTemplate;
        this.dispatchProperties = dispatchProperties;
    }

    public void dispatchInstruction(String payload, String correlationId) {
        String url = dispatchProperties.getLifecycleBaseUrl() + "/v1/instructions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", correlationId);
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Lifecycle dispatch failed: " + response.getStatusCode());
        }
    }
}
