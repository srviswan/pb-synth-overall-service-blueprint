package com.pbsynth.tradecapture.rules;

import java.util.Map;
import java.util.Objects;

final class RuleMatcher {
    private RuleMatcher() {
    }

    static boolean matches(Rule rule, Map<String, Object> context) {
        if (rule.getCriteria() == null || rule.getCriteria().isEmpty()) {
            return true;
        }
        return rule.getCriteria().stream().allMatch(criteria -> {
            Object actual = context.get(criteria.getField());
            String expected = criteria.getValue();
            String operator = criteria.getOperator() == null ? "EQ" : criteria.getOperator();
            return switch (operator) {
                case "EQ" -> Objects.equals(stringify(actual), expected);
                case "NEQ" -> !Objects.equals(stringify(actual), expected);
                case "IN" -> expected != null && actual != null && expected.contains(stringify(actual));
                default -> false;
            };
        });
    }

    private static String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
