package com.automation.elements;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class CustomElement {
    private WebElement element;
    private final WebDriver driver;
    private final By locator;
    private final WebDriverWait wait;
    
    public CustomElement(WebDriver driver, By locator) {
        this.driver = driver;
        this.locator = locator;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20)); 
    }
    
    public void click() {
        try {
            // Wait for element to be clickable
            element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            
            // Scroll element into view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            
            // Add a small wait after scrolling
            Thread.sleep(500);
            
            try {
                // Try normal click first
                element.click();
            } catch (ElementClickInterceptedException e) {
                // If normal click fails, try JavaScript click
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to click element: " + e.getMessage());
        }
    }
    
    public void sendKeys(String text) {
        try {
            // Wait for element to be visible and interactable
            element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            
            // Clear existing text
            element.clear();
            
            // Type the text
            element.sendKeys(text);
            
            // If element is a search box, press Enter
            if (element.getAttribute("type") != null && 
                element.getAttribute("type").equalsIgnoreCase("text")) {
                element.sendKeys(Keys.ENTER);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send keys to element: " + e.getMessage());
        }
    }
    
    public String getText() {
        try {
            element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return element.getText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get text from element: " + e.getMessage());
        }
    }
    
    public boolean isDisplayed() {
        try {
            element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getAttribute(String attribute) {
        try {
            element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return element.getAttribute(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get attribute from element: " + e.getMessage());
        }
    }
    
    public void waitForPresence() {
        try {
            element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new RuntimeException("Element not present after timeout: " + locator);
        }
    }
    
    public void waitForVisibility() {
        try {
            element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new RuntimeException("Element not visible after timeout: " + locator);
        }
    }
}
