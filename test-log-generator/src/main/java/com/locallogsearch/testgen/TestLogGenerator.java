/*
 * MIT License
 *
 * Copyright (c) 2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
    
    // Configuration
    private static Instant currentTimestamp = Instant.now();
    private static boolean useHistoricalTime = false;
    private static long maxLogs = 0; // 0 = unlimited
    private static long logsGenerated = 0;
    private static int delayMs = 0; // delay between log generations
    
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
    
    private static final String[] DATABASES = {
        "postgres", "mysql", "mongodb", "redis", "elasticsearch"
    };
    
    private static final String[] SERVICES = {
        "auth-service", "payment-service", "notification-service", 
        "analytics-service", "reporting-service"
    };
    
    private static final String[] CACHE_OPERATIONS = {
        "get", "set", "delete", "expire", "flush"
    };
    
    private static final String[] QUEUES = {
        "orders-queue", "notifications-queue", "analytics-queue", "email-queue"
    };
    
    private static final String[] API_VERSIONS = {
        "v1", "v2", "v3"
    };
    
    private static final String[] REGIONS = {
        "us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1"
    };
    
    private static final String[] CONTAINER_NAMES = {
        "api-server-1", "api-server-2", "worker-1", "worker-2", "scheduler-1"
    };
    
    private static final String[] STORAGE_TYPES = {
        "s3", "nfs", "local-disk"
    };
    
    private static final String[] SEARCH_INDICES = {
        "products", "users", "orders", "logs"
    };
    
    public static void main(String[] args) throws IOException {
        parseArguments(args);
        
        System.out.println("Starting Test Log Generator...");
        System.out.println("Logs will be written to: logs/");
        if (maxLogs > 0) {
            System.out.println("Max logs: " + maxLogs);
        }
        if (delayMs > 0) {
            System.out.println("Delay between logs: " + delayMs + "ms");
        }
        if (useHistoricalTime) {
            System.out.println("Using historical timestamps starting from: " + currentTimestamp);
        }
        System.out.println("Press Ctrl+C to stop.");
        System.out.println();
        
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
        if (maxLogs > 0 && logsGenerated >= maxLogs) {
            return;
        }
        
        // Generate different types of log messages based on random selection
        int logType = random.nextInt(100);
        
        if (logType < 12) {
            generateServiceLog();
        } else if (logType < 24) {
            generateDatabaseLog();
        } else if (logType < 34) {
            generateCacheLog();
        } else if (logType < 44) {
            generateAuthLog();
        } else if (logType < 54) {
            generatePaymentLog();
        } else if (logType < 64) {
            generateBusinessLog();
        } else if (logType < 74) {
            generateQueueLog();
        } else if (logType < 82) {
            generateApiGatewayLog();
        } else if (logType < 90) {
            generateContainerLog();
        } else if (logType < 95) {
            generateStorageLog();
        } else {
            generateSearchLog();
        }
        
        logsGenerated++;
        
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private static void generateServiceLog() {
        String level = getRandomLevel(false);
        String service = SERVICES[random.nextInt(SERVICES.length)];
        String operation = OPERATIONS[random.nextInt(OPERATIONS.length)];
        int duration = random.nextInt(500) + 10;
        int requestId = random.nextInt(100000);
        
        String message = String.format(
            "%s class=service level=%s service=%s operation=%s duration=%dms requestId=%d status=%s",
            Instant.now(),
            level,
            service,
            operation,
            duration,
            requestId,
            duration > 400 ? "slow" : "ok"
        );
        
        logAtLevel(appLog, level, message);
    }
    
    private static void generateDatabaseLog() {
        String level = getRandomLevel(false);
        String database = DATABASES[random.nextInt(DATABASES.length)];
        String query = random.nextBoolean() ? "SELECT" : "UPDATE";
        int queryTime = random.nextInt(1000) + 5;
        int rowsAffected = random.nextInt(1000);
        String table = "table_" + random.nextInt(10);
        
        String message = String.format(
            "%s class=database level=%s database=%s queryType=%s table=%s queryTime=%dms rowsAffected=%d pool=%s",
            Instant.now(),
            level,
            database,
            query,
            table,
            queryTime,
            rowsAffected,
            queryTime > 800 ? "exhausted" : "available"
        );
        
        logAtLevel(appLog, level, message);
    }
    
    private static void generateCacheLog() {
        String level = "DEBUG"; // Cache logs are typically debug
        String operation = CACHE_OPERATIONS[random.nextInt(CACHE_OPERATIONS.length)];
        String key = "key:" + random.nextInt(10000);
        boolean hit = random.nextInt(100) < 80; // 80% hit rate
        int ttl = random.nextInt(3600);
        
        String message = String.format(
            "%s class=cache level=%s operation=%s key=%s result=%s ttl=%d hitRate=%.2f",
            Instant.now(),
            level,
            operation,
            key,
            hit ? "hit" : "miss",
            ttl,
            0.75 + (random.nextDouble() * 0.2)
        );
        
        appLog.debug(message);
    }
    
    private static void generateAuthLog() {
        String level = getRandomLevel(false);
        String user = USERS[random.nextInt(USERS.length)];
        String action = random.nextBoolean() ? "login" : "logout";
        String ip = String.format("192.168.%d.%d", random.nextInt(255), random.nextInt(255));
        boolean success = random.nextInt(100) < 90; // 90% success rate
        String sessionId = "sess_" + random.nextInt(100000);
        
        String message;
        if (action.equals("login")) {
            message = String.format(
                "%s class=auth level=%s action=%s user=%s ip=%s success=%s sessionId=%s mfa=%s",
                Instant.now(),
                success ? level : "WARN",
                action,
                user,
                ip,
                success,
                sessionId,
                random.nextBoolean() ? "enabled" : "disabled"
            );
        } else {
            int sessionDuration = random.nextInt(7200);
            message = String.format(
                "%s class=auth level=%s action=%s user=%s sessionId=%s sessionDuration=%ds",
                Instant.now(),
                level,
                action,
                user,
                sessionId,
                sessionDuration
            );
        }
        
        logAtLevel(appLog, success ? level : "WARN", message);
    }
    
    private static void generatePaymentLog() {
        String level = getRandomLevel(false);
        String transactionId = "txn_" + random.nextInt(1000000);
        double amount = 10.0 + (random.nextDouble() * 990.0);
        String currency = random.nextBoolean() ? "USD" : (random.nextBoolean() ? "EUR" : "GBP");
        String gateway = random.nextBoolean() ? "stripe" : "paypal";
        String status = random.nextInt(100) < 95 ? "success" : "failed";
        
        String message = String.format(
            "%s class=payment level=%s transactionId=%s amount=%.2f currency=%s gateway=%s status=%s processingTime=%dms",
            Instant.now(),
            status.equals("failed") ? "ERROR" : level,
            transactionId,
            amount,
            currency,
            gateway,
            status,
            random.nextInt(3000) + 100
        );
        
        logAtLevel(appLog, status.equals("failed") ? "ERROR" : level, message);
    }
    
    private static void generateBusinessLog() {
        String level = getRandomLevel(false);
        String operation = OPERATIONS[random.nextInt(OPERATIONS.length)];
        String user = USERS[random.nextInt(USERS.length)];
        int duration = random.nextInt(500) + 10;
        int requestId = random.nextInt(100000);
        
        String message = String.format(
            "%s class=business level=%s operation=%s user=%s duration=%dms requestId=%d status=%s",
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
    
    private static void generateQueueLog() {
        String level = getRandomLevel(false);
        String queue = QUEUES[random.nextInt(QUEUES.length)];
        String action = random.nextInt(100) < 60 ? "enqueue" : "dequeue";
        int queueSize = random.nextInt(10000);
        int messageAge = random.nextInt(3600);
        int processingTime = random.nextInt(500);
        
        String message = String.format(
            "%s class=queue level=%s queue=%s action=%s queueSize=%d messageAge=%ds processingTime=%dms lag=%s",
            getTimestamp(),
            level,
            queue,
            action,
            queueSize,
            messageAge,
            processingTime,
            queueSize > 5000 ? "high" : "normal"
        );
        
        logAtLevel(appLog, level, message);
    }
    
    private static void generateApiGatewayLog() {
        String level = getRandomLevel(false);
        String endpoint = ENDPOINTS[random.nextInt(ENDPOINTS.length)];
        String version = API_VERSIONS[random.nextInt(API_VERSIONS.length)];
        String method = random.nextBoolean() ? "GET" : "POST";
        int statusCode = getRandomStatusCode();
        int responseTime = random.nextInt(2000) + 10;
        String clientId = "client_" + random.nextInt(100);
        
        String message = String.format(
            "%s class=api-gateway level=%s method=%s endpoint=%s version=%s status=%d responseTime=%dms clientId=%s rateLimitRemaining=%d",
            getTimestamp(),
            level,
            method,
            endpoint,
            version,
            statusCode,
            responseTime,
            clientId,
            random.nextInt(1000)
        );
        
        logAtLevel(appLog, level, message);
    }
    
    private static void generateContainerLog() {
        String level = getRandomLevel(false);
        String container = CONTAINER_NAMES[random.nextInt(CONTAINER_NAMES.length)];
        String region = REGIONS[random.nextInt(REGIONS.length)];
        double cpuPercent = random.nextDouble() * 100;
        double memoryMB = 100 + (random.nextDouble() * 1900);
        int restartCount = random.nextInt(5);
        
        String message = String.format(
            "%s class=container level=%s container=%s region=%s cpuPercent=%.2f memoryMB=%.2f restartCount=%d status=%s",
            getTimestamp(),
            level,
            container,
            region,
            cpuPercent,
            memoryMB,
            restartCount,
            cpuPercent > 90 ? "critical" : (cpuPercent > 70 ? "warning" : "healthy")
        );
        
        logAtLevel(appLog, level, message);
    }
    
    private static void generateStorageLog() {
        String level = getRandomLevel(false);
        String storageType = STORAGE_TYPES[random.nextInt(STORAGE_TYPES.length)];
        String operation = random.nextBoolean() ? "write" : "read";
        long bytes = random.nextInt(10000000);
        int latency = random.nextInt(500);
        String bucket = "bucket-" + random.nextInt(10);
        
        String message = String.format(
            "%s class=storage level=%s storage=%s operation=%s bucket=%s bytes=%d latency=%dms throughput=%.2fMBps",
            getTimestamp(),
            level,
            storageType,
            operation,
            bucket,
            bytes,
            latency,
            (bytes / 1024.0 / 1024.0) / (latency / 1000.0)
        );
        
        logAtLevel(appLog, level, message);
    }
    
    private static void generateSearchLog() {
        String level = getRandomLevel(false);
        String index = SEARCH_INDICES[random.nextInt(SEARCH_INDICES.length)];
        String queryType = random.nextBoolean() ? "term" : "match";
        int hits = random.nextInt(10000);
        int searchTime = random.nextInt(500);
        int shards = random.nextInt(5) + 1;
        
        String message = String.format(
            "%s class=search level=%s index=%s queryType=%s hits=%d searchTime=%dms shards=%d cacheHit=%s",
            getTimestamp(),
            level,
            index,
            queryType,
            hits,
            searchTime,
            shards,
            random.nextBoolean()
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
    
    private static Instant getTimestamp() {
        if (useHistoricalTime) {
            Instant ts = currentTimestamp;
            // Advance time by 1-10 seconds for historical mode
            currentTimestamp = currentTimestamp.plusSeconds(random.nextInt(10) + 1);
            return ts;
        }
        return Instant.now();
    }
    
    private static void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--max-logs":
                    if (i + 1 < args.length) {
                        maxLogs = Long.parseLong(args[++i]);
                    }
                    break;
                case "--delay":
                    if (i + 1 < args.length) {
                        delayMs = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--start-date":
                    if (i + 1 < args.length) {
                        currentTimestamp = Instant.parse(args[++i]);
                        useHistoricalTime = true;
                    }
                    break;
                case "--days-back":
                    if (i + 1 < args.length) {
                        int daysBack = Integer.parseInt(args[++i]);
                        currentTimestamp = Instant.now().minusSeconds(daysBack * 86400L);
                        useHistoricalTime = true;
                    }
                    break;
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
            }
        }
    }
    
    private static void printHelp() {
        System.out.println("Test Log Generator");
        System.out.println("\nUsage: java -jar test-log-generator.jar [OPTIONS]");
        System.out.println("\nOptions:");
        System.out.println("  --max-logs <N>      Generate N logs then stop (default: unlimited)");
        System.out.println("  --delay <ms>        Delay in milliseconds between logs (default: 0)");
        System.out.println("  --start-date <ISO>  Start with specific date (ISO format: 2025-01-01T00:00:00Z)");
        System.out.println("  --days-back <N>     Start N days ago and increment timestamps");
        System.out.println("  --help              Show this help message");
        System.out.println("\nExamples:");
        System.out.println("  # Generate 10000 logs");
        System.out.println("  java -jar test-log-generator.jar --max-logs 10000");
        System.out.println();
        System.out.println("  # Generate logs slowly (1 per second)");
        System.out.println("  java -jar test-log-generator.jar --delay 1000");
        System.out.println();
        System.out.println("  # Generate historical logs from 30 days ago");
        System.out.println("  java -jar test-log-generator.jar --days-back 30 --max-logs 50000");
        System.out.println();
        System.out.println("  # Generate logs from specific date");
        System.out.println("  java -jar test-log-generator.jar --start-date 2025-01-01T00:00:00Z --max-logs 5000");
    }
}
