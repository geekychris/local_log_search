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

import com.locallogsearch.core.search.SearchResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SummarizationService {
    
    private final ChatClient chatClient;
    
    // Maximum number of log entries to include in summarization
    private static final int MAX_LOGS_FOR_SUMMARY = 500;
    
    public enum SummaryType {
        OVERVIEW("General Overview", 
                "Provide a high-level overview of the log entries. Include: main themes, notable patterns, key statistics, and overall system health indicators."),
        
        ERRORS("Error Analysis", 
                "Focus on errors and warnings. Identify: error patterns, most common error types, affected components, potential root causes, and severity assessment."),
        
        PATTERNS("Pattern Detection", 
                "Identify recurring patterns and anomalies. Look for: repeated sequences, unusual behaviors, correlations between fields, and temporal patterns."),
        
        TIMELINE("Timeline Summary", 
                "Create a chronological summary. Highlight: key events over time, activity patterns, peak periods, and event sequences."),
        
        SECURITY("Security Analysis", 
                "Analyze from a security perspective. Identify: suspicious activities, authentication issues, access patterns, potential threats, and security-relevant events."),
        
        PERFORMANCE("Performance Analysis",
                "Focus on performance metrics. Analyze: response times, resource usage, slow operations, bottlenecks, and performance trends.");
        
        private final String displayName;
        private final String prompt;
        
        SummaryType(String displayName, String prompt) {
            this.displayName = displayName;
            this.prompt = prompt;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getPrompt() {
            return prompt;
        }
    }
    
    public SummarizationService(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }
    
    public String summarizeLogs(List<SearchResult> results, SummaryType summaryType, String userQuery) {
        if (results == null || results.isEmpty()) {
            return "No log entries to summarize.";
        }
        
        // Limit number of logs to analyze
        List<SearchResult> logsToAnalyze = results;
        boolean truncated = false;
        
        if (results.size() > MAX_LOGS_FOR_SUMMARY) {
            logsToAnalyze = results.subList(0, MAX_LOGS_FOR_SUMMARY);
            truncated = true;
        }
        
        // Build context from logs
        StringBuilder logContext = new StringBuilder();
        logContext.append("# Log Entries to Analyze\n\n");
        logContext.append("Query: ").append(userQuery).append("\n");
        logContext.append("Total entries: ").append(results.size());
        if (truncated) {
            logContext.append(" (analyzing first ").append(MAX_LOGS_FOR_SUMMARY).append(")");
        }
        logContext.append("\n\n");
        
        // Extract fields summary
        Map<String, Long> fieldFrequency = logsToAnalyze.stream()
            .filter(r -> r.getFields() != null)
            .flatMap(r -> r.getFields().keySet().stream())
            .collect(Collectors.groupingBy(f -> f, Collectors.counting()));
        
        if (!fieldFrequency.isEmpty()) {
            logContext.append("Available fields: ")
                      .append(String.join(", ", fieldFrequency.keySet()))
                      .append("\n\n");
        }
        
        // Include sample of logs with all details
        logContext.append("Log Samples:\n\n");
        int sampleSize = Math.min(100, logsToAnalyze.size());
        for (int i = 0; i < sampleSize; i++) {
            SearchResult result = logsToAnalyze.get(i);
            logContext.append("Log ").append(i + 1).append(":\n");
            if (result.getTimestamp() != null) {
                logContext.append("  Timestamp: ").append(result.getTimestamp()).append("\n");
            }
            logContext.append("  Raw: ").append(result.getRawText()).append("\n");
            if (result.getFields() != null && !result.getFields().isEmpty()) {
                logContext.append("  Fields:\n");
                result.getFields().forEach((key, value) -> 
                    logContext.append("    ").append(key).append(": ").append(value).append("\n")
                );
            }
            logContext.append("\n");
        }
        
        String systemPrompt = """
            You are a log analysis expert. Analyze the provided log entries and create a clear, actionable summary.
            
            Focus on:
            - Be concise but thorough
            - Highlight important findings
            - Use bullet points for clarity
            - Quantify when possible (e.g., "15 errors", "3 affected users")
            - Identify actionable items
            - Note any critical issues
            
            Format your response in markdown with appropriate headers and bullet points.
            """;
        
        String userMessage = summaryType.getPrompt() + "\n\n" + logContext.toString();
        
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage)
            ));
            
            String summary = chatClient.prompt(prompt).call().content();
            
            // Add disclaimer if truncated
            if (truncated) {
                summary = "**Note:** Analysis based on first " + MAX_LOGS_FOR_SUMMARY + 
                         " of " + results.size() + " total log entries.\n\n" + summary;
            }
            
            return summary;
            
        } catch (Exception e) {
            return "Error: Unable to generate summary. Make sure Ollama is running locally. " +
                   "Install with: brew install ollama && ollama pull llama3.2\n\nError details: " + 
                   e.getMessage();
        }
    }
    
    public int getMaxLogsForSummary() {
        return MAX_LOGS_FOR_SUMMARY;
    }
}
