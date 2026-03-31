package com.pbsynth.tradecapture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "tradecapture.dispatch")
public class DispatchProperties {
    private boolean workerEnabled = true;
    private long pollIntervalMs = 1000;
    private long baseBackoffMs = 1000;
    private List<Target> targets = defaultTargets();

    private static List<Target> defaultTargets() {
        Target lifecycle = new Target();
        lifecycle.setId("lifecycle-engine");
        lifecycle.setType("http");
        lifecycle.setUrl("http://localhost:8090/lifecycle-engine/v1/instructions");
        lifecycle.setEnabled(true);
        lifecycle.setMaxAttempts(5);
        lifecycle.setBatchSize(100);

        Target audit = new Target();
        audit.setId("audit-log");
        audit.setType("logging");
        audit.setEnabled(true);
        audit.setMaxAttempts(1);
        audit.setBatchSize(100);

        List<Target> defaults = new ArrayList<>();
        defaults.add(lifecycle);
        defaults.add(audit);
        return defaults;
    }

    public boolean isWorkerEnabled() {
        return workerEnabled;
    }

    public void setWorkerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public long getBaseBackoffMs() {
        return baseBackoffMs;
    }

    public void setBaseBackoffMs(long baseBackoffMs) {
        this.baseBackoffMs = baseBackoffMs;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = (targets == null || targets.isEmpty()) ? defaultTargets() : targets;
    }

    public static class Target {
        private String id;
        private String type;
        private String url;
        private boolean enabled = true;
        private int maxAttempts = 5;
        private int batchSize = 100;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
