package com.automation.factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeOptions;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.util.concurrent.TimeUnit;

public class WebDriverFactory {
    private static final Logger logger = LoggerFactory.getLogger(WebDriverFactory.class);
    private static final int IMPLICIT_WAIT = 10;
    private static final int PAGE_LOAD_TIMEOUT = 30;
    private static final int SCRIPT_TIMEOUT = 30;

    public static WebDriver createDriver(String browserType) {
        logger.info("Creating WebDriver for browser: {}", browserType);
        WebDriver driver = null;
        
        try {
            // Kill any existing browser processes first
            killExistingProcesses(browserType);
            
            switch (browserType.toLowerCase()) {
                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    configureFirefoxOptions(firefoxOptions);
                    driver = new FirefoxDriver(firefoxOptions);
                    break;
                    
                case "edge":
                    WebDriverManager.edgedriver().setup();
                    EdgeOptions edgeOptions = new EdgeOptions();
                    configureEdgeOptions(edgeOptions);
                    driver = new EdgeDriver(edgeOptions);
                    break;
                    
                default:
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions chromeOptions = new ChromeOptions();
                    configureChromeOptions(chromeOptions);
                    driver = new ChromeDriver(chromeOptions);
            }
            
            if (driver != null) {
                configureDriver(driver);
                logger.info("WebDriver created successfully");
            } else {
                throw new RuntimeException("Failed to create WebDriver instance");
            }
            
            return driver;
            
        } catch (Exception e) {
            logger.error("Failed to create WebDriver: {}", e.getMessage());
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ex) {
                    logger.error("Failed to quit WebDriver after creation failure: {}", ex.getMessage());
                }
            }
            throw new RuntimeException("WebDriver creation failed", e);
        }
    }
    
    private static void configureChromeOptions(ChromeOptions options) {
        // Basic arguments
        options.addArguments(Arrays.asList(
            "--start-maximized",
            "--disable-notifications",
            "--disable-popup-blocking",
            "--disable-infobars",
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--remote-allow-origins=*",
            "--disable-extensions",
            "--disable-web-security",
            "--disable-features=IsolateOrigins,site-per-process",
            "--ignore-certificate-errors",
            "--test-type",
            "--disable-blink-features=AutomationControlled",
            "--disable-browser-side-navigation",
            "--disable-site-isolation-trials"
        ));
        
        // Preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_setting_values.automatic_downloads", 1);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);
        
        // Exclude switches
        options.setExperimentalOption("excludeSwitches", 
            new String[]{"enable-automation", "enable-logging"});
        
        // Accept insecure certs
        options.setAcceptInsecureCerts(true);
        
        // Set binary location if needed
        String chromeBinary = System.getProperty("webdriver.chrome.binary");
        if (chromeBinary != null && !chromeBinary.isEmpty()) {
            options.setBinary(chromeBinary);
        }
    }
    
    private static void configureFirefoxOptions(FirefoxOptions options) {
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("dom.push.enabled", false);
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.helperApps.neverAsk.saveToDisk", 
            "application/pdf,application/x-pdf");
        options.setAcceptInsecureCerts(true);
        
        // Additional Firefox preferences
        options.addPreference("network.proxy.type", 0);
        options.addPreference("network.proxy.no_proxies_on", "localhost, 127.0.0.1");
        options.addPreference("browser.tabs.remote.autostart", false);
        options.addPreference("browser.tabs.remote.autostart.2", false);
    }
    
    private static void configureEdgeOptions(EdgeOptions options) {
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-infobars");
        options.setAcceptInsecureCerts(true);
    }
    
    private static void configureDriver(WebDriver driver) {
        try {
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(SCRIPT_TIMEOUT));
            driver.manage().deleteAllCookies();
            logger.info("Driver configuration completed successfully");
        } catch (Exception e) {
            logger.error("Failed to configure driver: {}", e.getMessage());
            throw e;
        }
    }
    
    private static void killExistingProcesses(String browser) {
        try {
            ProcessBuilder pb;
            if ("chrome".equalsIgnoreCase(browser)) {
                pb = new ProcessBuilder("taskkill", "/F", "/IM", "chromedriver.exe", "/IM", "chrome.exe");
            } else if ("firefox".equalsIgnoreCase(browser)) {
                pb = new ProcessBuilder("taskkill", "/F", "/IM", "geckodriver.exe", "/IM", "firefox.exe");
            } else if ("edge".equalsIgnoreCase(browser)) {
                pb = new ProcessBuilder("taskkill", "/F", "/IM", "MicrosoftEdge.exe", "/IM", "MicrosoftEdgeCP.exe", "/IM", "MicrosoftEdgeSH.exe");
            } else {
                return;
            }
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);
            
            // Small pause to ensure processes are fully terminated
            Thread.sleep(1000);
            
        } catch (Exception e) {
            logger.debug("Error killing existing processes: {}", e.getMessage());
        }
    }
}
