package com.automation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import com.automation.factory.WebDriverFactory;
import com.automation.utils.WebDriverCleanup;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    protected WebDriver driver;

    @BeforeMethod
    @Parameters({"browser"})
    public void setUp(String browser) {
        try {
            logger.info("Setting up test with browser: {}", browser);
            
            // Register cleanup hook
            WebDriverCleanup.registerShutdownHook();
            
            // Clean any existing drivers
            WebDriverCleanup.cleanupAllDrivers();
            
            // Setup WebDriverManager
            setupWebDriverManager(browser);
            
            // Create WebDriver
            driver = WebDriverFactory.createDriver(browser);
            
            // Verify driver
            if (driver == null) {
                throw new RuntimeException("WebDriver initialization failed - driver is null");
            }
            
            // Log success
            logger.info("WebDriver initialized successfully: {}", driver.getClass().getSimpleName());
            logSystemInfo();
            
        } catch (Exception e) {
            logger.error("Setup failed: {}", e.getMessage());
            throw new RuntimeException("Setup failed", e);
        }
    }

    private void setupWebDriverManager(String browser) {
        try {
            switch (browser.toLowerCase()) {
                case "chrome":
                    WebDriverManager.chromedriver().setup();
                    logger.info("ChromeDriver binary path: {}", WebDriverManager.chromedriver().getDownloadedDriverPath());
                    break;
                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    logger.info("FirefoxDriver binary path: {}", WebDriverManager.firefoxdriver().getDownloadedDriverPath());
                    break;
                case "edge":
                    WebDriverManager.edgedriver().setup();
                    logger.info("EdgeDriver binary path: {}", WebDriverManager.edgedriver().getDownloadedDriverPath());
                    break;
                default:
                    throw new RuntimeException(String.format("Unsupported browser type: %s", browser));
            }
        } catch (Exception e) {
            logger.error("Failed to setup WebDriverManager: {}", e.getMessage());
            throw new RuntimeException("WebDriverManager setup failed", e);
        }
    }

    private void logSystemInfo() {
        logger.info("System Information:");
        logger.info("OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        logger.info("Java Version: {}", System.getProperty("java.version"));
        logger.info("Browser: {}", driver.getClass().getSimpleName());
        
        // Log browser-specific information
        if (driver instanceof ChromeDriver) {
            logger.info("Chrome Version: {}", ((ChromeDriver) driver).getCapabilities().getBrowserVersion());
        } else if (driver instanceof FirefoxDriver) {
            logger.info("Firefox Version: {}", ((FirefoxDriver) driver).getCapabilities().getBrowserVersion());
        }
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            try {
                // Log final state
                logger.info("Quitting WebDriver");
                logger.info("Final URL before quit: {}", driver.getCurrentUrl());
                
                // Quit WebDriver
                driver.quit();
                driver = null;
                logger.info("WebDriver quit successfully");
                
                // Clean up drivers
                WebDriverCleanup.cleanupAllDrivers();
                
            } catch (Exception e) {
                logger.error("Error during WebDriver cleanup: {}", e.getMessage());
                // Force cleanup if normal quit fails
                try {
                    if (driver != null) {
                        driver.quit();
                    }
                } catch (Exception ex) {
                    logger.error("Force quit failed: {}", ex.getMessage());
                } finally {
                    driver = null;
                    WebDriverCleanup.cleanupAllDrivers();
                }
            }
        }
    }
}
