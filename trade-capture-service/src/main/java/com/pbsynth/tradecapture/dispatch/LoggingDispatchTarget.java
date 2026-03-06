package com.pbsynth.tradecapture.dispatch;

import com.pbsynth.tradecapture.config.DispatchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoggingDispatchTarget implements DispatchTarget {
    private static final Logger log = LoggerFactory.getLogger(LoggingDispatchTarget.class);

    @Override
    public String type() {
        return "logging";
    }

    @Override
    public DispatchResult dispatch(DispatchProperties.Target targetConfig, String envelopeJson, Map<String, String> headers) {
        log.info("Logging dispatch target id={} headers={} payload={}", targetConfig.getId(), headers, envelopeJson);
        return DispatchResult.ok();
    }
}
