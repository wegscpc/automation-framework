package com.automation.pages;

import com.automation.exceptions.AutomationException;
import com.automation.utils.ErrorTracker;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Objects;
import java.util.Arrays;
import java.time.Duration;

public class GoogleSearchResultsPage extends BasePage {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSearchResultsPage.class);
    
    // Main content containers
    private static final By[] CONTENT_LOCATORS = {
        By.cssSelector("#main"),
        By.cssSelector("#center_col"),
        By.cssSelector("#rcnt"),
        By.cssSelector("#search"),
        By.cssSelector("#rso"),
        By.cssSelector("div[data-async-context]"),
        By.cssSelector("div[data-hveid]"),
        By.cssSelector("div.v7W49e"),
        By.cssSelector("div.eKjLze"),
        By.cssSelector("div[jscontroller='d0DtYd']"),  
        By.cssSelector("div[jscontroller='SC7lYd']"),
        By.cssSelector("div.g-blk"),
        By.cssSelector("div.ULSxyf")
    };
    
    // Search results elements
    private static final By SEARCH_BOX = By.cssSelector("textarea[name='q'], input[name='q'], input[type='search'][role='combobox']");
    private static final By SEARCH_RESULTS = By.cssSelector(
        "div.g, " +
        "div[jscontroller='SC7lYd'], " +
        "div[data-sokoban-container], " +
        "div.MjjYud, " +
        "div.srKDX, " +
        "div[data-content-feature='1'], " +
        "div[jscontroller] > div[lang], " +  
        "div.v7W49e > div[jscontroller], " +
        "div.eKjLze > div[jscontroller], " +
        "div[jsname='Cpkphb'], " +
        "div[jsmodel], " +  
        "div.g-blk, " +
        "div.ULSxyf, " +
        "div[data-ved], " +
        "div.MjjYud > div, " +
        "div.g > div[lang]"
    );
    private static final By STATS = By.cssSelector("#result-stats, div.LHJvCe");

    public GoogleSearchResultsPage(WebDriver driver) {
        super(driver);
        waitForSearchResults();
        logger.info("Search results page initialized");
    }
    
    private void waitForSearchResults() {
        try {
            waitForPageToLoad();
            
            logger.debug("Waiting for search results to load...");
            
            // Wait for URL to indicate we're on a search page
            wait.until(driver -> {
                String url = driver.getCurrentUrl().toLowerCase();
                return url.contains("search?") || url.contains("&q=") || url.contains("?q=");
            });
            
            // Handle cookie consent first if present
            try {
                handleCookieConsent();
            } catch (Exception e) {
                logger.debug("Cookie consent handling skipped: {}", e.getMessage());
            }
            
            // Check for and handle language selection if present
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                By langDialog = By.cssSelector("div[aria-modal='true'], div[role='dialog']");
                
                WebElement langPrompt = shortWait.until(ExpectedConditions.presenceOfElementLocated(langDialog));
                if (langPrompt.isDisplayed()) {
                    // Look for English option with expanded selectors
                    List<By> englishSelectors = Arrays.asList(
                        By.xpath("//div[contains(text(), 'English')]"),
                        By.xpath("//a[contains(@href, 'hl=en')]"),
                        By.cssSelector("a[href*='hl=en']"),
                        By.xpath("//*[contains(translate(text(), 'ENGLISH', 'english'), 'english')]"),
                        By.cssSelector("[aria-label*='English']"),
                        By.cssSelector("[title*='English']")
                    );
                    
                    for (By selector : englishSelectors) {
                        try {
                            WebElement engOption = shortWait.until(ExpectedConditions.elementToBeClickable(selector));
                            if (engOption.isDisplayed()) {
                                // For Firefox, use Actions to click
                                if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                                    new org.openqa.selenium.interactions.Actions(driver)
                                        .moveToElement(engOption)
                                        .pause(Duration.ofMillis(100))
                                        .click()
                                        .perform();
                                } else {
                                    js.executeScript("arguments[0].click();", engOption);
                                }
                                waitForPageToLoad();
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Language selection handling skipped: {}", e.getMessage());
            }
            
            // Wait for any search box to be present (indicates page is interactive)
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(SEARCH_BOX));
            } catch (Exception e) {
                logger.debug("Search box not found, but continuing: {}", e.getMessage());
            }
            
            // Wait for content with multiple strategies
            boolean contentFound = false;
            
            // Strategy 1: Check main content containers
            try {
                contentFound = wait.until(driver -> {
                    for (By locator : CONTENT_LOCATORS) {
                        try {
                            WebElement element = driver.findElement(locator);
                            if (element.isDisplayed()) {
                                // For Firefox, use smooth scrolling
                                if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                                    js.executeScript(
                                        "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});",
                                        element);
                                    Thread.sleep(100);
                                }
                                return true;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    return false;
                });
            } catch (Exception e) {
                logger.debug("Content container strategy failed: {}", e.getMessage());
            }
            
            // Strategy 2: Check for individual results if no container found
            if (!contentFound) {
                try {
                    contentFound = wait.until(driver -> {
                        List<WebElement> results = driver.findElements(SEARCH_RESULTS);
                        return results.stream()
                            .filter(e -> {
                                try {
                                    return e.isDisplayed() && isElementInViewport(e);
                                } catch (Exception ex) {
                                    return false;
                                }
                            })
                            .count() > 0;
                    });
                } catch (Exception e) {
                    logger.debug("Individual results strategy failed: {}", e.getMessage());
                }
            }
            
            // Final check - if no content found through normal means, try JavaScript
            if (!contentFound) {
                // For Firefox, use a more reliable JavaScript check
                if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                    Boolean hasContent = (Boolean) js.executeScript(
                        "return Array.from(document.querySelectorAll('#search, #main, #center_col, #rso, .g, [data-hveid], [data-ved]')).some(e => {" +
                        "  var style = window.getComputedStyle(e);" +
                        "  return e.offsetHeight > 0 && style.display !== 'none' && style.visibility !== 'hidden';" +
                        "});"
                    );
                    
                    if (!hasContent) {
                        // One last refresh attempt
                        driver.navigate().refresh();
                        waitForPageToLoad();
                        
                        // Check again after refresh
                        hasContent = (Boolean) js.executeScript(
                            "return Array.from(document.querySelectorAll('#search, #main, #center_col, #rso, .g, [data-hveid], [data-ved]')).some(e => {" +
                            "  var style = window.getComputedStyle(e);" +
                            "  return e.offsetHeight > 0 && style.display !== 'none' && style.visibility !== 'hidden';" +
                            "});"
                        );
                    }
                    
                    if (!hasContent) {
                        throw new AutomationException("No search result content found after refresh");
                    }
                } else {
                    Boolean hasContent = (Boolean) js.executeScript(
                        "return document.querySelector('#search, #main, #center_col, #rso, .g') !== null && " +
                        "document.querySelector('#search, #main, #center_col, #rso, .g').offsetParent !== null;"
                    );
                    
                    if (!hasContent) {
                        // One last refresh attempt
                        driver.navigate().refresh();
                        waitForPageToLoad();
                        
                        // Check again after refresh
                        hasContent = (Boolean) js.executeScript(
                            "return document.querySelector('#search, #main, #center_col, #rso, .g') !== null && " +
                            "document.querySelector('#search, #main, #center_col, #rso, .g').offsetParent !== null;"
                        );
                        
                        if (!hasContent) {
                            throw new AutomationException("No search result content found after refresh");
                        }
                    }
                }
            }

            logger.info("Search results loaded successfully");
            
        } catch (Exception e) {
            throw new AutomationException(
                new ErrorTracker.Builder("Failed to initialize search results page")
                    .withCause(e)
                    .addContext("url", driver.getCurrentUrl())
                    .addContext("browser", driver.getClass().getSimpleName())
                    .build());
        }
    }
    
    private boolean isElementInViewport(WebElement element) {
        try {
            return (Boolean) js.executeScript(
                "var rect = arguments[0].getBoundingClientRect();" +
                "return (rect.top >= 0 && rect.left >= 0 && " +
                "rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && " +
                "rect.right <= (window.innerWidth || document.documentElement.clientWidth));",
                element
            );
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @deprecated Use hasSearchResults() instead
     */
    @Deprecated
    public boolean hasResults() {
        return hasSearchResults();
    }

    public boolean hasSearchResults() {
        try {
            // Wait for results with retry
            int retryCount = 0;
            while (retryCount < 3) {
                try {
                    // First check any content container is present
                    boolean contentFound = false;
                    for (By locator : CONTENT_LOCATORS) {
                        try {
                            WebElement content = driver.findElement(locator);
                            if (content.isDisplayed()) {
                                contentFound = true;
                                logger.debug("Found content container using: {}", locator);
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }

                    if (!contentFound) {
                        logger.warn("No content containers found");
                        if (++retryCount < 3) {
                            js.executeScript("window.stop();");
                            driver.navigate().refresh();
                            waitForPageToLoad();
                            handleCookieConsent();
                            continue;
                        }
                        return false;
                    }

                    // Try multiple strategies to find results
                    boolean hasResults = false;
                    
                    // Strategy 1: Check standard result containers
                    List<WebElement> results = driver.findElements(SEARCH_RESULTS);
                    int visibleResults = (int) results.stream()
                        .filter(WebElement::isDisplayed)
                        .count();

                    if (visibleResults > 0) {
                        logger.info("Found {} search results using standard selectors", visibleResults);
                        return true;
                    }
                    
                    // Strategy 2: Check for any elements with search result characteristics
                    try {
                        List<WebElement> alternativeResults = driver.findElements(
                            By.cssSelector(
                                "[data-hveid], " +  // Google result identifier
                                "[data-ved], " +    // Another Google result identifier
                                "div[jsmodel], " +  // Modern Google results
                                "div[jsdata], " +   // Dynamic content
                                "div[data-content-feature]"  // Feature content
                            )
                        );
                        
                        visibleResults = (int) alternativeResults.stream()
                            .filter(e -> {
                                try {
                                    return e.isDisplayed() && 
                                           e.findElements(By.cssSelector("a[href], h3, [role='heading']")).size() > 0;
                                } catch (Exception ex) {
                                    return false;
                                }
                            })
                            .count();
                            
                        if (visibleResults > 0) {
                            logger.info("Found {} search results using alternative selectors", visibleResults);
                            return true;
                        }
                    } catch (Exception e) {
                        logger.debug("Alternative result check failed: {}", e.getMessage());
                    }
                    
                    // Strategy 3: Use JavaScript to check for results
                    try {
                        Boolean jsResults = (Boolean) js.executeScript(
                            "return (document.querySelectorAll('[data-hveid], [data-ved], div[jsmodel], div[jsdata]').length > 0) && " +
                            "Array.from(document.querySelectorAll('[data-hveid], [data-ved], div[jsmodel], div[jsdata]')).some(e => {" +
                            "  var style = window.getComputedStyle(e);" +
                            "  return e.offsetHeight > 0 && style.display !== 'none' && style.visibility !== 'hidden' && " +
                            "  e.querySelector('a[href], h3, [role=\"heading\"]') !== null;" +
                            "});"
                        );
                        
                        if (Boolean.TRUE.equals(jsResults)) {
                            logger.info("Found search results using JavaScript check");
                            return true;
                        }
                    } catch (Exception e) {
                        logger.debug("JavaScript result check failed: {}", e.getMessage());
                    }

                    logger.debug("No results found with any strategy, retrying...");
                } catch (Exception e) {
                    logger.warn("Retry {} - Error checking results: {}", retryCount + 1, e.getMessage());
                }
                
                if (++retryCount < 3) {
                    js.executeScript("window.stop();");
                    driver.navigate().refresh();
                    waitForPageToLoad();
                    handleCookieConsent();
                    // Add a small delay before retry
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            // Log page state if no results found
            logger.error("No search results found after retries");
            logger.error("Current URL: {}", driver.getCurrentUrl());
            logger.error("Page title: {}", driver.getTitle());
            
            // Take screenshot for debugging
            try {
                if (driver instanceof TakesScreenshot) {
                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    logger.error("Page screenshot taken for debugging");
                }
            } catch (Exception e) {
                logger.error("Failed to take screenshot: {}", e.getMessage());
            }
            
            // Log page source for debugging
            try {
                String pageSource = driver.getPageSource();
                logger.error("Page source length: {} characters", pageSource.length());
                // Log first 1000 characters of page source
                if (pageSource.length() > 1000) {
                    logger.error("First 1000 chars of page source: {}", pageSource.substring(0, 1000));
                } else {
                    logger.error("Page source: {}", pageSource);
                }
            } catch (Exception e) {
                logger.error("Failed to log page source: {}", e.getMessage());
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error checking for search results: {}", e.getMessage(), e);
            return false;
        }
    }

    public List<String> getSearchResultTitles() {
        try {
            List<WebElement> results = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(SEARCH_RESULTS));
            return results.stream()
                .filter(WebElement::isDisplayed)
                .map(result -> {
                    try {
                        WebElement titleElement = result.findElement(By.cssSelector("h3, [role='heading']"));
                        return titleElement.getText();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting search result titles: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<String> getSearchResultUrls() {
        try {
            List<WebElement> results = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(SEARCH_RESULTS));
            return results.stream()
                .filter(WebElement::isDisplayed)
                .map(result -> {
                    try {
                        WebElement linkElement = result.findElement(By.cssSelector("a[href]:not([href=''])"));
                        return linkElement.getAttribute("href");
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting search result URLs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public int getNumberOfResults() {
        try {
            // First ensure we have results
            if (!hasSearchResults()) {
                return 0;
            }

            // Try to get the stats text
            WebElement statsElement = wait.until(ExpectedConditions.presenceOfElementLocated(STATS));
            String statsText = statsElement.getText();
            
            // Parse the number from different formats:
            // English: "About 123,456 results"
            // Spanish: "Aproximadamente 123.456 resultados"
            String numberStr = statsText.replaceAll("[^0-9]", "");
            
            if (!numberStr.isEmpty()) {
                return Integer.parseInt(numberStr);
            }

            // Fallback: count visible results
            List<WebElement> results = driver.findElements(SEARCH_RESULTS);
            return (int) results.stream()
                .filter(WebElement::isDisplayed)
                .count();

        } catch (Exception e) {
            logger.warn("Could not get number of results: {}", e.getMessage());
            // Return the count of visible results as fallback
            try {
                List<WebElement> results = driver.findElements(SEARCH_RESULTS);
                return (int) results.stream()
                    .filter(WebElement::isDisplayed)
                    .count();
            } catch (Exception ex) {
                logger.error("Could not count visible results: {}", ex.getMessage());
                return 0;
            }
        }
    }
}
