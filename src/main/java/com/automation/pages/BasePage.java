package com.automation.pages;

import com.automation.exceptions.AutomationException;
import com.automation.exceptions.ElementInteractionException;
import com.automation.utils.ErrorTracker;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;
    private static final Logger logger = LoggerFactory.getLogger(BasePage.class);

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.js = (JavascriptExecutor) driver;
    }

    protected void ensureBrowserAlive() {
        try {
            driver.getCurrentUrl();
        } catch (Exception e) {
            throw new AutomationException(
                new ErrorTracker.Builder("Browser appears to be dead or unresponsive")
                    .withCause(e)
                    .addContext("lastUrl", driver.getCurrentUrl())
                    .build()
            );
        }
    }

    protected void safeExecute(Runnable action, String actionDescription) {
        try {
            ensureBrowserAlive();
            action.run();
        } catch (Exception e) {
            throw new AutomationException(
                new ErrorTracker.Builder(String.format("Failed to %s", actionDescription))
                    .withCause(e)
                    .addContext("url", driver.getCurrentUrl())
                    .build()
            );
        }
    }

    protected <T> T safeExecute(Supplier<T> action, String actionDescription) {
        try {
            ensureBrowserAlive();
            return action.get();
        } catch (Exception e) {
            throw new AutomationException(
                new ErrorTracker.Builder(String.format("Failed to %s", actionDescription))
                    .withCause(e)
                    .addContext("url", driver.getCurrentUrl())
                    .build()
            );
        }
    }

    protected void waitForPageToLoad() {
        try {
            ensureBrowserAlive();

            // Wait for document ready state
            wait.until(driver -> {
                try {
                    return ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState")
                        .equals("complete");
                } catch (Exception e) {
                    return false;
                }
            });

            // Wait for jQuery if present
            try {
                Boolean jQueryDefined = (Boolean) js.executeScript("return typeof jQuery !== 'undefined'");
                if (Boolean.TRUE.equals(jQueryDefined)) {
                    wait.until(driver -> {
                        try {
                            return (Boolean) ((JavascriptExecutor) driver)
                                .executeScript("return jQuery.active === 0");
                        } catch (Exception e) {
                            return false;
                        }
                    });
                }
            } catch (Exception e) {
                logger.debug("jQuery check failed: {}", e.getMessage());
            }

            // Wait for all AJAX requests to complete
            try {
                wait.until(driver -> {
                    try {
                        return (Boolean) ((JavascriptExecutor) driver).executeScript(
                            "return (window.XMLHttpRequest.prototype.send === window.XMLHttpRequest.prototype.realSend) || " +
                            "(Array.prototype.slice.call(window.performance.getEntriesByType('resource')" +
                            ".filter(r => r.initiatorType === 'xmlhttprequest')" +
                            ".map(r => r.responseEnd)).every(t => t > 0);"
                        );
                    } catch (Exception e) {
                        return false;
                    }
                });
            } catch (Exception e) {
                logger.debug("AJAX check failed: {}", e.getMessage());
            }

            // Check for any error dialogs
            try {
                List<WebElement> errorDialogs = driver.findElements(
                    By.cssSelector("div[role='alert'], .error, .error-message, #error")
                );
                for (WebElement dialog : errorDialogs) {
                    if (dialog.isDisplayed()) {
                        String errorText = dialog.getText();
                        logger.warn("Error dialog found: {}", errorText);
                    }
                }
            } catch (Exception e) {
                logger.debug("Error dialog check failed: {}", e.getMessage());
            }

        } catch (Exception e) {
            throw new AutomationException(
                new ErrorTracker.Builder("Failed to wait for page load")
                    .withCause(e)
                    .addContext("url", driver.getCurrentUrl())
                    .build()
            );
        }
    }

    protected void waitForElementToBeVisible(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected void waitForElementToBeClickable(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected void scrollElementIntoView(WebElement element) {
        try {
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(500); // Wait for scroll animation
        } catch (Exception e) {
            throw new ElementInteractionException(
                "Failed to scroll element into view", 
                element, 
                e
            );
        }
    }

    protected void waitAndClick(WebElement element) {
        waitForElementToBeVisible(element);
        waitForElementToBeClickable(element);
        scrollElementIntoView(element);
        safeClick(element);
    }

    protected void waitAndSendKeys(WebElement element, String text) {
        waitForElementToBeVisible(element);
        scrollElementIntoView(element);
        element.clear();
        element.sendKeys(text);
    }

    protected void handleCookieConsent() {
        try {
            // Only try once per page load
            if (Boolean.TRUE.equals(js.executeScript("return window.__cookieHandled"))) {
                return;
            }
            js.executeScript("window.__cookieHandled = true;");

            // First check for the new "Before you continue" dialog
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                By beforeYouContinueDialog = By.xpath("//*[contains(text(), 'Before you continue to Google')]");
                WebElement dialog = shortWait.until(ExpectedConditions.presenceOfElementLocated(beforeYouContinueDialog));

                if (dialog.isDisplayed()) {
                    // Try the "Accept all" button first
                    List<By> acceptButtons = Arrays.asList(
                        By.xpath("//button[text()='Accept all']"),
                        By.xpath("//div[@role='dialog']//button[contains(@aria-label, 'Accept')]"),
                        By.xpath("//div[@role='dialog']//button[contains(., 'Accept all')]")
                    );

                    for (By buttonSelector : acceptButtons) {
                        try {
                            WebElement button = driver.findElement(buttonSelector);
                            if (button.isDisplayed()) {
                                // Try multiple click methods
                                try {
                                    js.executeScript("arguments[0].click();", button);
                                } catch (Exception e) {
                                    button.click();
                                }
                                // Wait for dialog to disappear
                                shortWait.until(ExpectedConditions.invisibilityOfElementLocated(beforeYouContinueDialog));
                                logger.info("Accepted cookies through 'Before you continue' dialog");
                                return;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("No 'Before you continue' dialog found");
            }

            // Fall back to checking for traditional cookie banners
            List<By> consentSelectors = Arrays.asList(
                // Traditional cookie banner buttons
                By.id("L2AGLb"),  // Google's standard cookie button ID
                By.cssSelector("button[aria-label*='Accept all']"),
                By.cssSelector("button[aria-label*='Aceptar todo']"),
                By.xpath("//button[contains(., 'Accept all')]"),
                By.xpath("//button[contains(., 'Aceptar todo')]"),

                // Generic consent form elements
                By.cssSelector("form[action*='consent'] button"),
                By.cssSelector("div[role='dialog'] button[role='button']"),
                By.cssSelector(".cookie-banner button"),
                By.cssSelector("[aria-label*='cookie'] button")
            );

            for (By selector : consentSelectors) {
                try {
                    WebElement button = driver.findElement(selector);
                    if (button.isDisplayed()) {
                        logger.debug("Found consent button using: {}", selector);
                        try {
                            js.executeScript(
                                "arguments[0].click(); " +
                                "arguments[0].dispatchEvent(new Event('change')); " +
                                "arguments[0].form && arguments[0].form.submit();", 
                                button);
                        } catch (Exception e) {
                            button.click();
                        }
                        logger.info("Clicked consent button successfully");
                        return;
                    }
                } catch (Exception e) {
                    logger.trace("Button not found with selector {}", selector);
                }
            }

            logger.debug("No cookie consent elements found");

        } catch (Exception e) {
            // Non-critical error, just log and continue
            logger.debug("Cookie consent handling skipped: {}", e.getMessage());
        }
    }

    protected WebElement findClickableElement(WebElement parent, By locator) {
        try {
            List<WebElement> elements = parent.findElements(locator);
            for (WebElement element : elements) {
                if (element.isDisplayed() && element.isEnabled()) {
                    // Try to ensure element is in viewport
                    scrollElementIntoView(element);
                    return element;
                }
            }
            return null;
        } catch (Exception e) {
            logger.trace("Element not found or not interactable: {}", locator);
            return null;
        }
    }

    protected void safeClick(WebElement element) {
        Exception lastException = null;
        boolean clicked = false;
        
        // Try regular click first
        try {
            element.click();
            clicked = true;
            return;
        } catch (Exception e) {
            lastException = e;
            logger.debug("Regular click failed: {}", e.getMessage());
        }
        
        // Try JavaScript click
        if (!clicked) {
            try {
                js.executeScript(
                    "arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));" +
                    "arguments[0].click();",
                    element
                );
                clicked = true;
                return;
            } catch (Exception e) {
                lastException = e;
                logger.debug("JavaScript click failed: {}", e.getMessage());
            }
        }

        // Try Actions click with move and scroll as last resort
        try {
            scrollElementIntoView(element);
            new org.openqa.selenium.interactions.Actions(driver)
                .moveToElement(element)
                .pause(Duration.ofMillis(100))
                .click()
                .perform();
            clicked = true;
            return;
        } catch (Exception e) {
            lastException = e;
            logger.debug("Actions click failed: {}", e.getMessage());
        }
        
        if (!clicked) {
            logger.error("All click attempts failed", lastException);
            throw new ElementInteractionException(
                "Failed to click element after multiple attempts",
                element,
                lastException
            );
        }
    }

    protected void waitForElementToDisappear(WebElement element) {
        try {
            wait.until(ExpectedConditions.invisibilityOf(element));
        } catch (Exception e) {
            throw new ElementInteractionException(
                "Element did not disappear as expected",
                element,
                e
            );
        }
    }

    protected void waitUntil(Function<WebDriver, Boolean> condition, String errorMessage) {
        try {
            wait.until(condition);
        } catch (Exception e) {
            throw new AutomationException(
                new ErrorTracker.Builder("Wait condition failed: " + errorMessage)
                    .withCause(e)
                    .addContext("url", driver.getCurrentUrl())
                    .build());
        }
    }

    protected boolean isElementPresent(By locator) {
        try {
            return !driver.findElements(locator).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    protected void highlightElement(WebElement element) {
        try {
            js.executeScript(
                "arguments[0].style.border='3px solid red';" +
                "setTimeout(function(){arguments[0].style.border='';}, 2000)", element);
        } catch (Exception e) {
            logger.trace("Failed to highlight element: {}", e.getMessage());
        }
    }
}
