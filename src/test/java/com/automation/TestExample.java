package com.automation;

import com.automation.pages.GoogleHomePage;
import com.automation.pages.GoogleSearchResultsPage;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExample extends BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(TestExample.class);
    
    @Test
    public void testGoogleSearch() {
        try {
            // Configure browser options
            if (driver instanceof org.openqa.selenium.chrome.ChromeDriver) {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--start-maximized");
                options.addArguments("--disable-notifications");
                options.addArguments("--disable-popup-blocking");
                options.addArguments("--disable-infobars");
                options.addArguments("--disable-gpu");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                ((org.openqa.selenium.chrome.ChromeDriver) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            } else if (driver instanceof org.openqa.selenium.firefox.FirefoxDriver) {
                FirefoxOptions options = new FirefoxOptions();
                options.addArguments("--start-maximized");
                options.addArguments("--disable-notifications");
                ((org.openqa.selenium.firefox.FirefoxDriver) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            }
            
            // Initialize and open Google homepage
            logger.info("Initializing Google homepage");
            GoogleHomePage homePage = new GoogleHomePage(driver);
            
            logger.info("Opening Google homepage");
            homePage.open();
            
            // Verify we're on Google homepage
            String title = homePage.getTitle();
            logger.info("Page title: {}", title);
            Assert.assertTrue(title.toLowerCase().contains("google"), 
                "Expected Google in title but found: " + title);
            
            // Perform search
            logger.info("Performing search for 'Selenium WebDriver'");
            GoogleSearchResultsPage resultsPage = homePage.search("Selenium WebDriver");
            
            // Verify search results
            logger.info("Verifying search results");
            Assert.assertTrue(resultsPage.hasResults(), 
                "No search results found for 'Selenium WebDriver'");
            
            // Get and verify number of results
            int resultCount = resultsPage.getNumberOfResults();
            logger.info("Found {} search results", resultCount);
            Assert.assertTrue(resultCount > 0, 
                "Expected some search results but found: " + resultCount);
            
            // Log success
            logger.info("Test completed successfully");
            
        } catch (Exception e) {
            logger.error("Test failed unexpectedly", e);
            Assert.fail("Test failed unexpectedly: " + e.getMessage());
        }
    }
}
