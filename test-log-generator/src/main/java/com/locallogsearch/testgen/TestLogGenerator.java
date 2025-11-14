package com.locallogsearch.testgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestLogGenerator {
    private static final Logger appLog = LoggerFactory.getLogger("app");
    private static final Logger accessLog = LoggerFactory.getLogger("access");
    private static final Logger errorLog = LoggerFactory.getLogger("error");
    
    private static final Random random = new Random();
    
    private static final String[] OPERATIONS = {
        "processOrder", "validateUser", "fetchData", "updateCache", 
        "sendNotification", "calculatePrice", "generateReport"
    };
    
    private static final String[] USERS = {
        "user123", "admin", "john.doe", "jane.smith", "bot-crawler"
    };
    
    private static final String[] ENDPOINTS = {
        "/api/orders", "/api/users", "/api/products", "/api/reports", 
        "/health", "/metrics", "/login", "/logout"
    };
    
    private static final String[] ERROR_MESSAGES = {
        "Connection timeout", "Invalid credentials", "Resource not found",
        "Database connection failed", "Out of memory", "Permission denied",
        "Rate limit exceeded", "Invalid request format"
    };
    
    public static void main(String[] args) throws IOException {
        System.out.println("Starting Test Log Generator...");
        System.out.println("Logs will be written to: logs/");
        System.out.println("Press Ctrl+C to stop.");
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        
        // Application logs - mixed format with key=value
        scheduler.scheduleAtFixedRate(() -> generateApplicationLog(), 0, 1, TimeUnit.SECONDS);
        
        // Access logs - HTTP format
        scheduler.scheduleAtFixedRate(() -> generateAccessLog(), 0, 500, TimeUnit.MILLISECONDS);
        
        // Error logs - burst occasionally
        scheduler.scheduleAtFixedRate(() -> {
            if (random.nextInt(10) < 3) { // 30% chance
                generateErrorBurst();
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        // Keep running
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            scheduler.shutdown();
        }
    }
    
    private static void generateApplicationLog() {
        String level = getRandomLevel(false);
        String operation = OPERATIONS[random.nextInt(OPERATIONS.length)];
        String user = USERS[random.nextInt(USERS.length)];
        int duration = random.nextInt(500) + 10;
        int requestId = random.nextInt(100000);
        
        String message = String.format(
            "%s level=%s operation=%s user=%s duration=%dms requestId=%d status=%s",
            Instant.now(),
            level,
            operation,
            user,
            duration,
            requestId,
            duration > 400 ? "slow" : "ok"
        );
        
        logAtLevel(appLog, level, message);
    }
    
    private static void generateAccessLog() {
        String endpoint = ENDPOINTS[random.nextInt(ENDPOINTS.length)];
        String method = random.nextBoolean() ? "GET" : "POST";
        int statusCode = getRandomStatusCode();
        int responseTime = random.nextInt(300) + 10;
        String ip = String.format("192.168.%d.%d", random.nextInt(255), random.nextInt(255));
        
        String message = String.format(
            "%s method=%s endpoint=%s status=%d responseTime=%dms ip=%s",
            Instant.now(),
            method,
            endpoint,
            statusCode,
            responseTime,
            ip
        );
        
        accessLog.info(message);
    }
    
    private static void generateErrorBurst() {
        int errorCount = random.nextInt(5) + 1;
        for (int i = 0; i < errorCount; i++) {
            String errorMsg = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
            String component = OPERATIONS[random.nextInt(OPERATIONS.length)];
            
            String message = String.format(
                "%s level=ERROR component=%s error=\"%s\" threadId=%d",
                Instant.now(),
                component,
                errorMsg,
                Thread.currentThread().getId()
            );
            
            errorLog.error(message);
            
            try {
                Thread.sleep(random.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private static String getRandomLevel(boolean errorBias) {
        if (errorBias) {
            return "ERROR";
        }
        
        int rand = random.nextInt(100);
        if (rand < 60) return "INFO";
        if (rand < 80) return "DEBUG";
        if (rand < 95) return "WARN";
        return "ERROR";
    }
    
    private static int getRandomStatusCode() {
        int rand = random.nextInt(100);
        if (rand < 70) return 200;
        if (rand < 85) return 201;
        if (rand < 90) return 404;
        if (rand < 95) return 500;
        return 503;
    }
    
    private static void logAtLevel(Logger logger, String level, String message) {
        switch (level) {
            case "DEBUG" -> logger.debug(message);
            case "INFO" -> logger.info(message);
            case "WARN" -> logger.warn(message);
            case "ERROR" -> logger.error(message);
            default -> logger.info(message);
        }
    }
}
