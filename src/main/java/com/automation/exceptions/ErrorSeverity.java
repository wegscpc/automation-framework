package com.automation.exceptions;

/**
 * Represents different severity levels for errors, similar to Google's error tracking.
 */
public enum ErrorSeverity {
    INFO,       // Non-critical issues that don't affect functionality
    WARNING,    // Issues that may affect functionality but don't stop execution
    ERROR,      // Critical issues that prevent normal operation
    FATAL       // Severe issues that require immediate termination
}
