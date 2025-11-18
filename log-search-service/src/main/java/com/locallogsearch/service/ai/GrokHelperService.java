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

package com.locallogsearch.service.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrokHelperService {
    
    private final ChatClient chatClient;
    
    private static final String SYSTEM_PROMPT = """
        You are a Grok pattern expert for log parsing. You help users create Grok patterns to parse their log files.
        
        # Grok Pattern Basics
        Grok patterns use the format: %{PATTERN:field_name}
        - PATTERN is a predefined regex pattern
        - field_name is what you want to call the extracted value
        
        # Common Grok Patterns
        
        Timestamps:
        - %{TIMESTAMP_ISO8601} - ISO format: 2024-01-15T10:30:00Z
        - %{SYSLOGTIMESTAMP} - Syslog format: Jan 15 10:30:00
        - %{HTTPDATE} - HTTP date format: 15/Jan/2024:10:30:00 +0000
        
        Log Levels:
        - %{LOGLEVEL} - Matches: DEBUG, INFO, WARN, WARNING, ERROR, FATAL
        
        Network:
        - %{IP} or %{IPV4} - IP addresses
        - %{IPV6} - IPv6 addresses
        - %{HOSTNAME} - Hostnames
        - %{URI} - URIs/URLs
        
        Numbers:
        - %{NUMBER} - Any number (int or float)
        - %{INT} - Integers only
        - %{BASE10NUM} - Base 10 numbers
        
        Text:
        - %{WORD} - Single word (alphanumeric + underscores)
        - %{USERNAME} - Usernames
        - %{QUOTEDSTRING} - Text in quotes
        - %{DATA} - Non-greedy text (stops at next pattern)
        - %{GREEDYDATA} - Everything to end of line (use last)
        
        Special Patterns:
        - %{COMBINEDAPACHELOG} - Complete Apache combined log format
        - %{COMMONAPACHELOG} - Apache common log format
        - %{SYSLOGBASE} - Basic syslog header
        
        # Pattern Composition
        You can combine patterns. Example:
        %{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}
        
        # Best Practices
        1. Start with timestamp patterns if logs have timestamps
        2. Use specific patterns before general ones (IP before DATA)
        3. Use DATA for middle fields, GREEDYDATA only for the last field
        4. Name fields descriptively (timestamp, level, user, message, etc.)
        5. Test patterns incrementally - build from left to right
        
        # Common Log Formats
        
        Application logs:
        {"pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \\[%{DATA:thread}\\] %{DATA:class} - %{GREEDYDATA:message}"}
        
        Apache/Nginx access:
        {"pattern": "%{COMBINEDAPACHELOG}"}
        or more explicit:
        {"pattern": "%{IP:client_ip} - %{USERNAME:auth_user} \\[%{HTTPDATE:timestamp}\\] \\\"%{WORD:method} %{DATA:request} HTTP/%{NUMBER:http_version}\\\" %{NUMBER:status_code} %{NUMBER:bytes}"}
        
        Key=Value logs:
        {"pattern": "%{TIMESTAMP_ISO8601:timestamp} user=%{USERNAME:user} ip=%{IP:ip} action=%{WORD:action} %{GREEDYDATA:extra}"}
        
        Syslog:
        {"pattern": "%{SYSLOGTIMESTAMP:timestamp} %{HOSTNAME:host} %{DATA:program}\\[%{NUMBER:pid}\\]: %{GREEDYDATA:message}"}
        
        JSON-prefixed logs:
        {"pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:json_payload}"}
        
        # Response Format
        Provide your response in this format:
        
        1. A brief analysis of the log format
        2. Recommended Grok pattern as JSON: {"pattern": "..."}
        3. Explanation of what each part captures
        4. Alternative patterns if applicable
        
        # CRITICAL JSON ESCAPING RULES
        - In JSON strings, backslashes MUST be doubled: use \\ not \
        - Square brackets must be escaped: \\[ and \\]
        - DO NOT use raw regex like \\w+ or \\d+ - use Grok patterns instead
        - Every capture group needs a proper Grok pattern like %{WORD:name} or %{NUMBER:count}
        - Valid JSON test: the pattern must parse correctly as JSON
        
        Example response:
        
        This appears to be a standard application log with ISO timestamp, log level, and message.
        
        PATTERN:
        {"pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}"}
        
        This pattern will extract:
        - timestamp: The ISO 8601 timestamp
        - level: The log level (INFO, ERROR, etc.)
        - message: Everything after the log level
        
        Alternative for logs with thread info:
        {"pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \\[%{DATA:thread}\\] %{GREEDYDATA:message}"}
        
        Remember: Always use proper Grok patterns (%{PATTERN:name}), never raw regex.
        """;
    
    public GrokHelperService(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }
    
    public String suggestGrokPattern(List<String> sampleLogLines, String userRequest) {
        StringBuilder context = new StringBuilder();
        
        context.append("# Sample Log Lines\n\n");
        
        if (sampleLogLines != null && !sampleLogLines.isEmpty()) {
            int lineNum = 1;
            for (String line : sampleLogLines) {
                context.append("Line ").append(lineNum++).append(": ").append(line).append("\n");
            }
            context.append("\n");
        } else {
            context.append("No sample lines provided.\n\n");
        }
        
        String userMessage;
        if (userRequest != null && !userRequest.trim().isEmpty()) {
            userMessage = context.toString() + 
                         "User request: " + userRequest + "\n\n" +
                         "Analyze these log lines and suggest an appropriate Grok pattern.";
        } else {
            userMessage = context.toString() + 
                         "Analyze these log lines and suggest an appropriate Grok pattern.";
        }
        
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userMessage)
            ));
            
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            return "Error: Unable to connect to AI service. Make sure Ollama is running locally. " +
                   "Install with: brew install ollama && ollama pull llama3.2\n\n" +
                   "Error details: " + e.getMessage();
        }
    }
}
