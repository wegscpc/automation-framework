package com.automation.exceptions;

import com.automation.utils.ErrorTracker;
import org.openqa.selenium.WebElement;

/**
 * Exception thrown when interaction with a web element fails.
 */
public class ElementInteractionException extends AutomationException {
    
    public ElementInteractionException(String message, WebElement element) {
        super(new ErrorTracker.Builder(message)
            .addElementContext("element", element)
            .build());
    }

    public ElementInteractionException(String message, WebElement element, Throwable cause) {
        super(new ErrorTracker.Builder(message)
            .withCause(cause)
            .addElementContext("element", element)
            .build());
    }
}
