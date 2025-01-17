package com.automation.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class PageBuilder<T> {
    private final WebDriver driver;
    private WebDriverWait wait;
    private String url;
    private int timeout = 10;
    private final Class<T> pageClass;
    
    public PageBuilder(WebDriver driver, Class<T> pageClass) {
        this.driver = driver;
        this.pageClass = pageClass;
    }
    
    public PageBuilder<T> withUrl(String url) {
        this.url = url;
        return this;
    }
    
    public PageBuilder<T> withTimeout(int seconds) {
        this.timeout = seconds;
        return this;
    }
    
    public PageBuilder<T> withWait() {
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        return this;
    }
    
    public WebDriver getDriver() {
        return driver;
    }
    
    public WebDriverWait getWait() {
        return wait;
    }
    
    public String getUrl() {
        return url;
    }
}
