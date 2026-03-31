package com.pbsynth.tradecapture.health;

import com.pbsynth.tradecapture.config.DispatchProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component("lifecycleEngine")
public class LifecycleEngineHealthIndicator implements HealthIndicator {
    private final DispatchProperties dispatchProperties;
    private final RestTemplate restTemplate;

    public LifecycleEngineHealthIndicator(DispatchProperties dispatchProperties, RestTemplate restTemplate) {
        this.dispatchProperties = dispatchProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        String url = dispatchProperties.getTargets().stream()
                .filter(t -> "lifecycle-engine".equals(t.getId()) && t.getUrl() != null)
                .map(DispatchProperties.Target::getUrl)
                .findFirst()
                .orElse(null);
        if (url == null) {
            return Health.unknown().withDetail("reason", "lifecycle-engine target not configured").build();
        }
        try {
            restTemplate.optionsForAllow(url);
            return Health.up().withDetail("target", url).build();
        } catch (RestClientException ex) {
            return Health.down(ex).withDetail("target", url).build();
        }
    }
}
