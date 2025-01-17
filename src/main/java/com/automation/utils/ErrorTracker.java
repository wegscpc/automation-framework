package com.automation.utils;

import org.openqa.selenium.WebElement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks error context information similar to Google's error tracking system.
 * Provides rich context for debugging automation failures.
 */
public class ErrorTracker {
    private final Map<String, Object> contextData;
    private final String message;
    private final Throwable cause;

    private ErrorTracker(Builder builder) {
        this.contextData = builder.contextData;
        this.message = builder.message;
        this.cause = builder.cause;
    }

    public Map<String, Object> getContextData() {
        return new LinkedHashMap<>(contextData);
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("Error: ").append(message)
            .append("\nContext: {");
        
        contextData.forEach((key, value) -> 
            sb.append("\n  ").append(key).append(": ").append(value));
        
        sb.append("\n}");
        
        if (cause != null) {
            sb.append("\nCaused by: ").append(cause.toString());
        }
        
        return sb.toString();
    }

    public static class Builder {
        private final Map<String, Object> contextData = new LinkedHashMap<>();
        private String message;
        private Throwable cause;

        public Builder(String message) {
            this.message = message;
        }

        public Builder withCause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Builder addContext(String key, Object value) {
            if (value != null) {
                contextData.put(key, value);
            }
            return this;
        }

        public Builder addElementContext(String prefix, WebElement element) {
            try {
                addContext(prefix + ".tagName", element.getTagName());
                addContext(prefix + ".text", element.getText());
                addContext(prefix + ".location", element.getLocation());
                addContext(prefix + ".displayed", element.isDisplayed());
                addContext(prefix + ".enabled", element.isEnabled());
            } catch (Exception e) {
                // Element might be stale, add what we can
                addContext(prefix + ".error", "Failed to get element details: " + e.getMessage());
            }
            return this;
        }

        public ErrorTracker build() {
            return new ErrorTracker(this);
        }
    }
}
