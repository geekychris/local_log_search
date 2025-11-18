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
import java.util.Map;

@Service
public class SqlHelperService {
    
    private final ChatClient chatClient;
    
    private static final String SYSTEM_PROMPT = """
        You are a SQL query writing assistant. You help users write SQL queries for their H2 database tables.
        
        # SQL Basics
        - Use SELECT * FROM table_name to get all columns
        - Use SELECT column1, column2 FROM table_name for specific columns
        - Use WHERE clause for filtering: WHERE column = 'value'
        - Use JOIN for combining tables
        - Use GROUP BY for aggregations
        - Use ORDER BY for sorting
        - Use LIMIT to restrict results
        
        # Common Patterns
        
        Basic query:
        SELECT * FROM my_table LIMIT 100;
        
        Filtering:
        SELECT * FROM my_table WHERE status = 'active';
        SELECT * FROM my_table WHERE amount > 100;
        SELECT * FROM my_table WHERE created_at >= '2024-01-01';
        
        Aggregations:
        SELECT user, COUNT(*) as count FROM my_table GROUP BY user;
        SELECT user, SUM(amount) as total FROM my_table GROUP BY user ORDER BY total DESC;
        SELECT status, AVG(duration) as avg_duration FROM my_table GROUP BY status;
        
        Joins (INNER JOIN):
        SELECT a.*, b.column_name 
        FROM table_a a
        INNER JOIN table_b b ON a.id = b.a_id;
        
        Joins (LEFT JOIN):
        SELECT u.user, COUNT(e.id) as error_count
        FROM users u
        LEFT JOIN errors e ON u.user = e.user
        GROUP BY u.user;
        
        Multiple conditions:
        SELECT * FROM my_table 
        WHERE status = 'active' 
        AND amount > 100 
        ORDER BY created_at DESC
        LIMIT 10;
        
        # Response Format
        Structure your response as follows:
        
        1. Start with "QUERY:" followed by the complete SQL query on the next line
        2. If there are multiple options, show each as "QUERY:" followed by the query
        3. After each query, add a brief explanation
        4. Use proper SQL syntax (uppercase keywords recommended but not required)
        
        Example response format:
        QUERY:
        SELECT user, COUNT(*) as error_count
        FROM error_logs
        WHERE timestamp >= '2024-01-01'
        GROUP BY user
        ORDER BY error_count DESC
        LIMIT 10;
        
        This finds the top 10 users with the most errors since January 1st, 2024.
        
        # Important Notes
        - Always check table and column names provided in the context
        - Use table schemas to ensure column names are correct
        - For joins, identify common columns between tables
        - Add LIMIT clause for safety when querying large tables
        - Use appropriate data types in WHERE clauses (strings in quotes, numbers without)
        """;
    
    public SqlHelperService(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }
    
    public String helpWithSql(String userRequest, String currentQuery, 
                             List<Map<String, Object>> tableSchemas) {
        StringBuilder context = new StringBuilder();
        
        // Build context from table schemas
        if (tableSchemas != null && !tableSchemas.isEmpty()) {
            context.append("\n# Available Tables and Columns\n\n");
            for (Map<String, Object> table : tableSchemas) {
                String tableName = (String) table.get("tableName");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
                
                context.append("Table: ").append(tableName).append("\n");
                context.append("Columns:\n");
                if (columns != null) {
                    for (Map<String, Object> column : columns) {
                        String colName = (String) column.get("columnName");
                        String dataType = (String) column.get("dataType");
                        context.append("  - ").append(colName)
                               .append(" (").append(dataType).append(")\n");
                    }
                }
                context.append("\n");
            }
        }
        
        String userMessage;
        if (currentQuery != null && !currentQuery.trim().isEmpty()) {
            userMessage = String.format(
                "%sCurrent query: %s\n\nUser request: %s\n\nProvide an improved or modified SQL query.",
                context.toString(), currentQuery, userRequest
            );
        } else {
            userMessage = String.format(
                "%sUser request: %s\n\nProvide a SQL query to accomplish this.",
                context.toString(), userRequest
            );
        }
        
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userMessage)
            ));
            
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            return "Error: Unable to connect to AI service. Make sure Ollama is running locally. " +
                   "Install with: brew install ollama && ollama pull llama3.2";
        }
    }
}
