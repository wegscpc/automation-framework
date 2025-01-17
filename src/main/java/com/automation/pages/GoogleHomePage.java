package com.automation.pages;

import com.automation.exceptions.AutomationException;
import com.automation.utils.ErrorTracker;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

public class GoogleHomePage extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(GoogleHomePage.class);
    private static final String GOOGLE_URL = "https://www.google.com";
    private static final By SEARCH_BOX = By.cssSelector("textarea[name='q'], input[name='q']");
    private static final By SEARCH_BUTTON = By.cssSelector("input[name='btnK'], button[name='btnK']");
    private static final By GOOGLE_LOGO = By.cssSelector("img[alt='Google']");
    private static final By BODY = By.tagName("body");
    private static final By CONSENT_DIALOG = By.cssSelector("div[role='dialog']");
    private static final By CONSENT_REJECT_ALL = By.cssSelector("button[aria-label*='Reject all']");
    private static final By CONSENT_ACCEPT_ALL = By.cssSelector("button[aria-label*='Accept all']");
    private static final By SEARCH_FORM = By.cssSelector("form[role='search']");
    private static final By LANGUAGE_LINKS = By.cssSelector("a[href*='setprefs']");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    public GoogleHomePage(WebDriver driver) {
        super(driver);
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.js = (JavascriptExecutor) driver;
        logger.info("GoogleHomePage initialized with driver: " + driver.getClass().getSimpleName());
    }

    public void open() {
        try {
            logger.info("Attempting to open Google homepage: " + GOOGLE_URL);
            driver.get(GOOGLE_URL);
            waitForPageLoadComplete();
            handleCookieConsent();
            verifyGooglePage();
        } catch (Exception e) {
            logger.error("Failed to open Google homepage: " + e.getMessage(), e);
            logPageState();
            throw new RuntimeException("Failed to open Google homepage", e);
        }
    }

    public GoogleSearchResultsPage search(String searchText) {
        return safeExecute(() -> {
            logger.info("Performing search for: {}", searchText);
            
            // Ensure page is in a stable state
            waitForPageToLoad();
            
            // Handle language preferences and overlays
            handleLanguagePreferences();
            handleOverlays();
            
            // Wait for and find search box with retry
            final WebElement searchBox = findInteractableSearchBox();
            if (searchBox == null) {
                throw new AutomationException(
                    new ErrorTracker.Builder("Could not find interactable search box after multiple attempts")
                        .addContext("searchText", searchText)
                        .build()
                );
            }

            // Clear and enter text
            clearAndEnterText(searchBox, searchText);
            
            // Submit search with retries
            return submitSearch(searchBox);
            
        }, "perform search");
    }
    
    private void clearAndEnterText(WebElement searchBox, String searchText) {
        safeExecute(() -> {
            // Clear existing text with retry
            boolean cleared = false;
            for (int i = 0; i < 3 && !cleared; i++) {
                try {
                    // Try JavaScript clear first
                    js.executeScript("arguments[0].value = '';", searchBox);
                    searchBox.clear(); // Backup clear
                    
                    cleared = wait.until(d -> searchBox.getAttribute("value").isEmpty());
                } catch (Exception e) {
                    if (i == 2) throw e;
                    logger.debug("Clear attempt {} failed: {}", i + 1, e.getMessage());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AutomationException("Thread interrupted while waiting between clear attempts", ie);
                    }
                }
            }
            
            if (!cleared) {
                throw new AutomationException(
                    new ErrorTracker.Builder("Failed to clear search box after multiple attempts")
                        .addContext("searchText", searchText)
                        .build()
                );
            }
            
            // Type text character by character with verification
            StringBuilder enteredText = new StringBuilder();
            for (char c : searchText.toCharArray()) {
                boolean charEntered = false;
                for (int attempt = 0; attempt < 3 && !charEntered; attempt++) {
                    try {
                        // Try regular sendKeys first
                        searchBox.sendKeys(String.valueOf(c));
                        enteredText.append(c);
                        
                        // Verify character was entered
                        charEntered = wait.until(d -> 
                            searchBox.getAttribute("value").equals(enteredText.toString())
                        );
                        
                        if (!charEntered && attempt < 2) {
                            // Try JavaScript as backup
                            js.executeScript(
                                "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                                searchBox, enteredText.toString()
                            );
                            
                            charEntered = wait.until(d -> 
                                searchBox.getAttribute("value").equals(enteredText.toString())
                            );
                        }
                    } catch (Exception e) {
                        if (attempt == 2) throw e;
                        logger.debug("Character entry attempt {} failed for '{}': {}", 
                            attempt + 1, c, e.getMessage());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new AutomationException("Thread interrupted while waiting between character entry attempts", ie);
                        }
                    }
                }
                
                if (!charEntered) {
                    throw new AutomationException(
                        new ErrorTracker.Builder("Failed to enter character after multiple attempts")
                            .addContext("character", String.valueOf(c))
                            .addContext("searchText", searchText)
                            .addContext("enteredSoFar", enteredText.toString())
                            .build()
                    );
                }
                
                try {
                    Thread.sleep(50); // Small delay between characters
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AutomationException("Thread interrupted while waiting between characters", e);
                }
            }
            
            // Final verification
            if (!wait.until(d -> searchText.equals(searchBox.getAttribute("value")))) {
                throw new AutomationException(
                    new ErrorTracker.Builder("Failed to verify final search text")
                        .addContext("expected", searchText)
                        .addContext("actual", searchBox.getAttribute("value"))
                        .build()
                );
            }
        }, "clear and enter search text");
    }
    
    private GoogleSearchResultsPage submitSearch(WebElement searchBox) {
        Exception lastException = null;
        int attempts = 0;
        final int maxAttempts = 3;
        
        while (attempts < 3) {
            try {
                // Method 1: Try pressing Enter key (most reliable)
                try {
                    // For Firefox, focus the element first
                    if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                        js.executeScript("arguments[0].focus();", searchBox);
                        Thread.sleep(100);
                    }
                    
                    // First try regular sendKeys
                    searchBox.sendKeys(Keys.RETURN);
                    GoogleSearchResultsPage results = waitForSearchResults();
                    if (results != null) return results;
                    
                    // If that didn't work, try JavaScript enter key events
                    js.executeScript(
                        "arguments[0].dispatchEvent(new KeyboardEvent('keypress', {key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true}));" +
                        "arguments[0].dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true}));" +
                        "arguments[0].dispatchEvent(new KeyboardEvent('keyup', {key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true}));",
                        searchBox
                    );
                    results = waitForSearchResults();
                    if (results != null) return results;
                    
                    // Try submitting the form directly
                    WebElement form = searchBox.findElement(By.xpath("./ancestor::form"));
                    form.submit();
                    results = waitForSearchResults();
                    if (results != null) return results;
                } catch (Exception e) {
                    logger.debug("Enter key submission failed: {}", e.getMessage());
                }
            } catch (Exception e) {
                lastException = e;
                logger.debug("Enter key submission failed (attempt {}): {}", attempts + 1, e.getMessage());
            }
            
            try {
                // Method 2: Find and click search button
                List<WebElement> buttons = driver.findElements(SEARCH_BUTTON);
                for (WebElement button : buttons) {
                    if (button.isDisplayed() && button.isEnabled()) {
                        // Try to ensure button is clickable
                        js.executeScript(
                            "arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});" +
                            "arguments[0].style.border = '2px solid red';", // Highlight for debugging
                            button
                        );
                        Thread.sleep(100);
                        
                        // For Firefox, move to element first
                        if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                            new org.openqa.selenium.interactions.Actions(driver)
                                .moveToElement(button)
                                .pause(Duration.ofMillis(100))
                                .perform();
                        }
                        
                        // Try JavaScript click first
                        try {
                            js.executeScript(
                                "arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));" +
                                "arguments[0].click();",
                                button
                            );
                            GoogleSearchResultsPage results = waitForSearchResults();
                            if (results != null) return results;
                        } catch (Exception e) {
                            // Try regular click as fallback
                            button.click();
                            GoogleSearchResultsPage results = waitForSearchResults();
                            if (results != null) return results;
                        }
                    }
                }
            } catch (Exception e) {
                lastException = e;
                logger.debug("Button click failed (attempt {}): {}", attempts + 1, e.getMessage());
            }
            
            try {
                // Method 3: JavaScript form submit
                WebElement form = driver.findElement(SEARCH_FORM);
                
                // For Firefox, try both form.submit() and JavaScript submit
                if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                    try {
                        form.submit();
                        GoogleSearchResultsPage results = waitForSearchResults();
                        if (results != null) return results;
                    } catch (Exception e) {
                        logger.debug("Form submit failed, trying JavaScript: {}", e.getMessage());
                    }
                }
                
                js.executeScript(
                    "arguments[0].dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));" +
                    "arguments[0].submit();",
                    form
                );
                GoogleSearchResultsPage results = waitForSearchResults();
                if (results != null) return results;
                
                // Last resort - try to submit via JavaScript function call
                js.executeScript(
                    "document.forms[0].submit();" +
                    "document.querySelector('form[role=\"search\"]').submit();"
                );
                results = waitForSearchResults();
                if (results != null) return results;
            } catch (Exception e) {
                lastException = e;
                logger.debug("Form submit failed (attempt {}): {}", attempts + 1, e.getMessage());
            }
            
            attempts++;
            if (attempts < maxAttempts) {
                logger.info("Search submission attempt {} failed, retrying...", attempts);
                try {
                    Thread.sleep(1000); // Wait before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AutomationException("Thread interrupted while waiting between search attempts", e);
                }
            }
        }
        
        throw new AutomationException(
            new ErrorTracker.Builder("All search submission methods failed")
                .withCause(lastException)
                .addContext("attempts", attempts)
                .addContext("lastException", lastException != null ? lastException.getMessage() : "none")
                .addContext("browser", driver.getClass().getSimpleName())
                .addContext("url", driver.getCurrentUrl())
                .build()
        );
    }
    
    private GoogleSearchResultsPage waitForSearchResults() {
        try {
            // Wait for URL to change to search results
            wait.until(driver -> {
                try {
                    String url = driver.getCurrentUrl().toLowerCase();
                    return url.contains("search?") || url.contains("&q=") || url.contains("?q=");
                } catch (Exception e) {
                    return false;
                }
            });
            
            // Initialize results page with extended timeout
            WebDriverWait extendedWait = new WebDriverWait(driver, Duration.ofSeconds(20));
            try {
                waitForPageToLoad();
                waitForJQueryIfPresent();
                return new GoogleSearchResultsPage(driver);
            } catch (Exception e) {
                logger.debug("Initial results page load failed: {}", e.getMessage());
                // If initialization fails, try refreshing once
                driver.navigate().refresh();
                waitForPageToLoad();
                waitForJQueryIfPresent();
                return new GoogleSearchResultsPage(driver);
            }
        } catch (Exception e) {
            logger.debug("Failed to wait for search results: {}", e.getMessage());
            return null; // Return null to allow retry
        }
    }

    public String getTitle() {
        try {
            logger.info("Getting page title");
            String title = driver.getTitle();
            logger.info("Page title: " + title);
            return title;
        } catch (Exception e) {
            logger.error("Failed to get page title: " + e.getMessage());
            throw new RuntimeException("Failed to get page title", e);
        }
    }

    private void waitForPageLoadComplete() {
        try {
            // Wait for body to be present
            wait.until(ExpectedConditions.presenceOfElementLocated(BODY));
            
            // Wait for document ready state
            wait.until(driver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState").equals("complete"));
            
            // Wait for any pending XHR requests
            wait.until(driver -> {
                try {
                    return (Boolean) ((JavascriptExecutor) driver).executeScript(
                        "return (window.jQuery != null && jQuery.active === 0) || true;"
                    );
                } catch (Exception e) {
                    return true;
                }
            });
            
            // Additional wait for dynamic content
            try {
                Thread.sleep(1000); // Give time for dynamic content to load
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AutomationException("Thread interrupted while waiting for page load", e);
            }
            
            logger.debug("Page load completed successfully");
        } catch (Exception e) {
            logger.warn("Error while waiting for page load: {}", e.getMessage());
        }
    }

    private void verifyGooglePage() {
        try {
            // Verify URL
            String currentUrl = driver.getCurrentUrl();
            if (!currentUrl.startsWith("https://www.google.")) {
                throw new RuntimeException("Not on Google page. Current URL: " + currentUrl);
            }

            // Verify Google logo is present
            wait.until(ExpectedConditions.presenceOfElementLocated(GOOGLE_LOGO));

            // Verify search box is present and interactable
            wait.until(ExpectedConditions.elementToBeClickable(SEARCH_BOX));

            logger.info("Google page verified successfully");
        } catch (Exception e) {
            logger.error("Failed to verify Google page: " + e.getMessage());
            logPageState();
            throw new RuntimeException("Failed to verify Google page", e);
        }
    }

    private void handleLanguagePreferences() {
        try {
            // Check if language selection is present
            List<WebElement> languageLinks = driver.findElements(LANGUAGE_LINKS);
            if (!languageLinks.isEmpty()) {
                // Try to find English link
                Optional<WebElement> englishLink = languageLinks.stream()
                    .filter(link -> link.getText().trim().equalsIgnoreCase("English"))
                    .findFirst();
                
                if (englishLink.isPresent()) {
                    // Click the English link using JavaScript
                    js.executeScript("arguments[0].click();", englishLink.get());
                    waitForPageLoadComplete();
                    logger.info("Switched interface to English");
                }
            }
        } catch (Exception e) {
            logger.debug("No language selection needed: {}", e.getMessage());
        }
    }

    @Override
    protected void handleCookieConsent() {
        try {
            // Wait for dialog with increased timeout
            WebDriverWait dialogWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement dialog = dialogWait.until(ExpectedConditions.presenceOfElementLocated(CONSENT_DIALOG));

            // Try reject all first
            try {
                WebElement rejectButton = dialog.findElement(CONSENT_REJECT_ALL);
                rejectButton.click();
                logger.info("Clicked 'Reject all' in cookie consent dialog");
                return;
            } catch (Exception e) {
                logger.debug("No 'Reject all' button found: " + e.getMessage());
            }

            // Try accept all as fallback
            try {
                WebElement acceptButton = dialog.findElement(CONSENT_ACCEPT_ALL);
                acceptButton.click();
                logger.info("Clicked 'Accept all' in cookie consent dialog");
            } catch (Exception e) {
                logger.debug("No 'Accept all' button found: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.debug("No cookie consent dialog found: " + e.getMessage());
            // Continue as dialog might not appear in all regions
        }
    }

    private void logPageState() {
        try {
            logger.error("Current URL: " + driver.getCurrentUrl());
            logger.error("Page Title: " + driver.getTitle());
            logger.error("Page Source: " + driver.getPageSource());
        } catch (Exception e) {
            logger.error("Failed to log page state: " + e.getMessage());
        }
    }

    private void handleOverlays() {
        try {
            // Try to find and close any visible overlays
            List<By> overlaySelectors = Arrays.asList(
                By.cssSelector("div[role='dialog']"),
                By.cssSelector(".modal"),
                By.cssSelector(".overlay"),
                By.cssSelector("[aria-modal='true']")
            );
            
            for (By selector : overlaySelectors) {
                try {
                    List<WebElement> overlays = driver.findElements(selector);
                    for (WebElement overlay : overlays) {
                        if (overlay.isDisplayed()) {
                            // Try to close using close button first
                            try {
                                WebElement closeButton = overlay.findElement(
                                    By.cssSelector("button[aria-label*='close'], .close, .dismiss")
                                );
                                if (closeButton.isDisplayed()) {
                                    closeButton.click();
                                    continue;
                                }
                            } catch (Exception e) {
                                // No close button found
                            }
                            
                            // Try to remove overlay using JavaScript
                            js.executeScript("arguments[0].remove();", overlay);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Failed to handle overlay: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Error handling overlays: {}", e.getMessage());
        }
    }
    
    private void waitForJQueryIfPresent() {
        try {
            Boolean jQueryDefined = (Boolean) js.executeScript("return typeof jQuery !== 'undefined'");
            if (Boolean.TRUE.equals(jQueryDefined)) {
                wait.until(d -> (Boolean) js.executeScript("return jQuery.active === 0"));
            }
        } catch (Exception e) {
            logger.debug("jQuery check failed: {}", e.getMessage());
        }
    }

    private WebElement findInteractableSearchBox() {
        int attempts = 0;
        while (attempts < 3) {
            try {
                return wait.until(driver -> {
                    try {
                        WebElement element = driver.findElement(SEARCH_BOX);
                        // Check if element is truly interactable
                        return element.isDisplayed() && element.isEnabled() && 
                               (Boolean) js.executeScript(
                                   "var elem = arguments[0];" +
                                   "var style = window.getComputedStyle(elem);" +
                                   "return !!(elem.offsetWidth || elem.offsetHeight || elem.getClientRects().length) && " +
                                   "style.visibility !== 'hidden' && style.display !== 'none';", 
                                   element) ? element : null;
                    } catch (Exception e) {
                        return null;
                    }
                });
            } catch (Exception e) {
                attempts++;
                if (attempts < 3) {
                    // Try to remove any overlays and refresh if needed
                    js.executeScript(
                        "document.querySelectorAll('div[role=\"dialog\"], .modal, .overlay').forEach(e => e.remove());"
                    );
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        throw new AutomationException(
                            new ErrorTracker.Builder("Thread interrupted while waiting between search box attempts")
                                .withCause(e1)
                                .addContext("attempts", attempts)
                                .build()
                        );
                    }
                }
            }
        }
        return null;
    }
}
