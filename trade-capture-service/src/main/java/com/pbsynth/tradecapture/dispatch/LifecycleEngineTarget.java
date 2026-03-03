package com.pbsynth.tradecapture.dispatch;

import com.pbsynth.tradecapture.config.DispatchProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class LifecycleEngineTarget implements DispatchTarget {
    private final RestTemplate restTemplate;

    public LifecycleEngineTarget(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String type() {
        return "http";
    }

    @Override
    public DispatchResult dispatch(DispatchProperties.Target targetConfig, String envelopeJson, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            headers.forEach(httpHeaders::set);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    targetConfig.getUrl(),
                    new HttpEntity<>(envelopeJson, httpHeaders),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return DispatchResult.ok();
            }
            return DispatchResult.fail("Non-success status: " + response.getStatusCode());
        } catch (Exception ex) {
            return DispatchResult.fail(ex.getMessage());
        }
    }
}
