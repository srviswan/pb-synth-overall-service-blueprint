package com.pbsynth.tradecapture.dispatch;

public record DispatchResult(
        boolean success,
        String errorMessage
) {
    public static DispatchResult ok() {
        return new DispatchResult(true, null);
    }

    public static DispatchResult fail(String errorMessage) {
        return new DispatchResult(false, errorMessage);
    }
}
