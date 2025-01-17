package com.automation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

public class WebDriverCleanup {
    private static final Logger logger = LoggerFactory.getLogger(WebDriverCleanup.class);
    private static boolean cleanupRegistered = false;

    public static void registerShutdownHook() {
        if (!cleanupRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Running WebDriver cleanup on shutdown");
                cleanupAllDrivers();
            }));
            cleanupRegistered = true;
        }
    }

    public static void cleanupAllDrivers() {
        // Kill browser processes
        killBrowserProcesses();
        
        // Clean driver files with retries
        cleanDriverFiles();
    }

    private static void killBrowserProcesses() {
        String[] processes = {
            "chromedriver.exe", "chrome.exe",
            "geckodriver.exe", "firefox.exe",
            "msedgedriver.exe", "msedge.exe"
        };

        for (String process : processes) {
            try {
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", process);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.debug("Error killing process {}: {}", process, e.getMessage());
            }
        }

        // Wait for processes to fully terminate
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void cleanDriverFiles() {
        Path wdmPath = Paths.get(System.getProperty("user.dir"), ".wdm");
        int maxRetries = 3;
        int retryDelay = 2000; // 2 seconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (Files.exists(wdmPath)) {
                    // Use FileVisitor for better control over deletion
                    Files.walkFileTree(wdmPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            try {
                                Files.setAttribute(file, "dos:readonly", false);
                                Files.deleteIfExists(file);
                            } catch (IOException e) {
                                logger.debug("Could not delete file {}: {}", file, e.getMessage());
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            try {
                                Files.deleteIfExists(dir);
                            } catch (IOException e) {
                                logger.debug("Could not delete directory {}: {}", dir, e.getMessage());
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });

                    // Check if cleanup was successful
                    if (!Files.exists(wdmPath)) {
                        logger.info("WebDriver files cleaned successfully");
                        return;
                    }
                }
            } catch (Exception e) {
                logger.debug("Cleanup attempt {} failed: {}", attempt, e.getMessage());
            }

            // Wait before retry
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        logger.warn("Could not fully clean WebDriver files after {} attempts", maxRetries);
    }
}
