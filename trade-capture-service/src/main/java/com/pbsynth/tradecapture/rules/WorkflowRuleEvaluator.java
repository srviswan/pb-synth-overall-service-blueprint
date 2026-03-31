package com.pbsynth.tradecapture.rules;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WorkflowRuleEvaluator {
    public boolean evaluate(Rule rule, Map<String, Object> context) {
        return RuleMatcher.matches(rule, context);
    }
}
