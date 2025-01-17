package com.automation.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks error context information similar to Google's __closure__error__context__.
 */
public class ErrorContext {
    private final Map<String, Object> contextData;
    private ErrorSeverity severity;

    public ErrorContext() {
        this.contextData = new HashMap<>();
        this.severity = ErrorSeverity.ERROR;
    }

    public ErrorContext addData(String key, Object value) {
        contextData.put(key, value);
        return this;
    }

    public ErrorContext setSeverity(ErrorSeverity severity) {
        this.severity = severity;
        return this;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public Map<String, Object> getContextData() {
        return new HashMap<>(contextData);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("Severity: ").append(severity)
            .append(", Data: {");
        
        contextData.forEach((key, value) -> 
            sb.append(key).append("=").append(value).append(", "));
        
        if (!contextData.isEmpty()) {
            sb.setLength(sb.length() - 2); // Remove last ", "
        }
        
        return sb.append("}").toString();
    }
}
