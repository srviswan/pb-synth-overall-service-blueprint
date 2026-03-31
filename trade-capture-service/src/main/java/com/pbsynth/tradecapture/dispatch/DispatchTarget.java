package com.pbsynth.tradecapture.dispatch;

import com.pbsynth.tradecapture.config.DispatchProperties;

import java.util.Map;

public interface DispatchTarget {
    String type();

    DispatchResult dispatch(DispatchProperties.Target targetConfig, String envelopeJson, Map<String, String> headers);
}
