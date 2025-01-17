package com.automation.exceptions;

import com.automation.utils.ErrorTracker;

/**
 * Custom exception for automation framework that includes rich context tracking.
 */
public class AutomationException extends RuntimeException {
    private final ErrorTracker errorTracker;

    public AutomationException(ErrorTracker errorTracker) {
        super(errorTracker.getMessage(), errorTracker.getCause());
        this.errorTracker = errorTracker;
    }

    public AutomationException(String message) {
        this(new ErrorTracker.Builder(message).build());
    }

    public AutomationException(String message, Throwable cause) {
        this(new ErrorTracker.Builder(message).withCause(cause).build());
    }

    public ErrorTracker getErrorTracker() {
        return errorTracker;
    }

    @Override
    public String getMessage() {
        return errorTracker.toString();
    }
}
