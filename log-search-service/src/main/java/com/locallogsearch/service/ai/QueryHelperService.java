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

import com.locallogsearch.core.pipe.PipeCommand;
import com.locallogsearch.core.pipe.commands.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryHelperService {
    
    private final ChatClient chatClient;
    private final String systemPrompt;
    
    private static final String BASE_SYSTEM_PROMPT = """
        You are a helpful assistant for writing log search queries. The system uses Lucene query syntax followed by optional pipe commands.
        
        # Query Structure
        A complete query has two parts: `<lucene_query> | <pipe_commands>`
        
        Examples:
        - `level:ERROR | stats count by component`
        - `(level:ERROR OR level:WARN) | stats count by ip`
        - `message:timeout AND service:api | timechart span=5m count`
        
        # Lucene Query Syntax (First Part, Before the Pipe)
        
        IMPORTANT: Lucene queries use colon (:) for field searches, NOT equals (=) or hyphens (-).
        
        Field searches (most common):
        - `level:ERROR` - Find entries where level field equals ERROR
        - `user:alice` - Find entries where user is alice
        - `status:500` - Find entries with status 500
        - `ip:192.168.1.1` - Find entries from specific IP
        
        Boolean operators (MUST be uppercase: AND, OR, NOT):
        - `level:ERROR OR level:INFO` - Either ERROR or INFO logs
        - `level:ERROR AND service:api` - ERROR logs from api service
        - `level:ERROR AND NOT user:admin` - Errors not from admin
        - `(level:ERROR OR level:WARN) AND service:database` - Grouped conditions
        
        Text search (searches message field by default):
        - `error` - Search for word "error" anywhere
        - `"connection timeout"` - Exact phrase search
        - `message:"database error"` - Phrase in specific field
        
        Wildcards:
        - `user:admin*` - Starts with "admin"
        - `error?` - Single character wildcard
        - `*.log` - Ends with ".log"
        
        Ranges:
        - `amount:[100 TO 500]` - Between 100 and 500 (inclusive)
        - `timestamp:[2024-01-01 TO 2024-12-31]` - Date range
        - `duration:{0 TO 100}` - Exclusive range (not including 0 or 100)
        
        Special:
        - `*` - Match all entries
        - `field:*` - All entries where field exists
        
        WRONG SYNTAX (do not use):
        - `level-error` - Wrong! Use `level:ERROR`
        - `level=error` - Wrong! Use `level:ERROR`
        - `level-error or level-info` - Wrong! Use `level:ERROR OR level:INFO`
        - `search (level-error)` - Wrong! Don't nest searches, use `level:ERROR`
        
        %s
        
        # Multi-Stage Pipelines
        Pipe commands process the Lucene search results:
        - `level:ERROR | stats count by component` - Count errors per component
        - `(level:ERROR OR level:WARN) | filter duration > 100 | stats avg(duration) by service` - Multi-stage
        - `* | timechart span=1h count by level` - Time series of all logs by level
        
        # Response Format
        When a user asks for help, structure your response as follows:
        
        1. Start with "QUERY:" followed by the complete query on the next line
        2. If there are multiple options, show each as "QUERY:" followed by the query
        3. After each query, add a brief explanation
        4. Use proper Lucene syntax (field:value, uppercase AND/OR/NOT)
        
        Example response format:
        QUERY:
        (level:ERROR OR level:INFO) | stats count by ip, operation
        
        This searches for both ERROR and INFO level logs, then groups them by IP address and operation to show the distribution.
        
        If you have alternative queries, format each one the same way:
        QUERY:
        <alternative query>
        <explanation>
        """;
    
    public QueryHelperService(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.systemPrompt = buildSystemPrompt();
    }
    
    /**
     * Build the system prompt by collecting documentation from all pipe command implementations.
     */
    private String buildSystemPrompt() {
        StringBuilder pipeCommandsDocs = new StringBuilder();
        pipeCommandsDocs.append("# Pipe Commands (used with | syntax)\n\n");
        
        // Create instances of each command to collect their documentation
        List<PipeCommand> commands = new ArrayList<>();
        commands.add(new FilterCommand("field", "=", "value")); // Dummy instance for docs
        commands.add(TransformCommand.rename("old", "new")); // Using factory method
        commands.add(new StatsCommand(List.of("count"), List.of()));
        commands.add(new ChartCommand("bar", List.of("count"), List.of()));
        commands.add(new TimeChartCommand("1h", List.of("count"), null));
        commands.add(new ExportCommand("table", null, null, false));
        
        for (PipeCommand command : commands) {
            pipeCommandsDocs.append(command.getAIDocumentation()).append("\n\n");
        }
        
        return String.format(BASE_SYSTEM_PROMPT, pipeCommandsDocs.toString());
    }
    
    public String helpWithQuery(String userRequest, String currentQuery) {
        String userMessage;
        if (currentQuery != null && !currentQuery.trim().isEmpty()) {
            userMessage = String.format("Current query: %s\n\nUser request: %s\n\nProvide an improved or modified query.", 
                currentQuery, userRequest);
        } else {
            userMessage = String.format("User request: %s\n\nProvide a query to accomplish this.", userRequest);
        }
        
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage)
            ));
            
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            return "Error: Unable to connect to AI service. Make sure Ollama is running locally. " +
                   "Install with: brew install ollama && ollama pull llama2";
        }
    }
}
