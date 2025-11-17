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

package com.locallogsearch.core.pipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Splunk-style pipe queries
 * Example: "level:ERROR | stats count by user | sort -count"
 */
public class PipeQueryParser {
    
    public static class ParsedQuery {
        private final String baseQuery;
        private final List<PipeCommandSpec> pipeCommands;
        
        public ParsedQuery(String baseQuery, List<PipeCommandSpec> pipeCommands) {
            this.baseQuery = baseQuery;
            this.pipeCommands = pipeCommands;
        }
        
        public String getBaseQuery() {
            return baseQuery;
        }
        
        public List<PipeCommandSpec> getPipeCommands() {
            return pipeCommands;
        }
        
        public boolean hasPipes() {
            return !pipeCommands.isEmpty();
        }
    }
    
    public static class PipeCommandSpec {
        private final String command;
        private final Map<String, String> params;
        private final List<String> args;
        
        public PipeCommandSpec(String command, Map<String, String> params, List<String> args) {
            this.command = command;
            this.params = params;
            this.args = args;
        }
        
        public String getCommand() {
            return command;
        }
        
        public Map<String, String> getParams() {
            return params;
        }
        
        public List<String> getArgs() {
            return args;
        }
        
        public String getParam(String key, String defaultValue) {
            return params.getOrDefault(key, defaultValue);
        }
    }
    
    /**
     * Parse a query with pipes
     */
    public static ParsedQuery parse(String fullQuery) {
        if (fullQuery == null || fullQuery.trim().isEmpty()) {
            return new ParsedQuery("*", new ArrayList<>());
        }
        
        // Split by pipe, but not pipes inside quotes
        String[] parts = splitByPipe(fullQuery);
        
        String baseQuery = parts[0].trim();
        if (baseQuery.isEmpty()) {
            baseQuery = "*";
        }
        
        List<PipeCommandSpec> commands = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            PipeCommandSpec spec = parsePipeCommand(parts[i].trim());
            if (spec != null) {
                commands.add(spec);
            }
        }
        
        return new ParsedQuery(baseQuery, commands);
    }
    
    /**
     * Split by pipe character, respecting quotes
     */
    private static String[] splitByPipe(String query) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '|' && !inQuotes) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }
    
    /**
     * Parse a single pipe command
     * Examples:
     *   "stats count by user"
     *   "timechart span=1h count"
     *   "sort -count"
     *   "head 100"
     */
    private static PipeCommandSpec parsePipeCommand(String commandStr) {
        if (commandStr.isEmpty()) {
            return null;
        }
        
        String[] tokens = tokenize(commandStr);
        if (tokens.length == 0) {
            return null;
        }
        
        String command = tokens[0];
        Map<String, String> params = new HashMap<>();
        List<String> args = new ArrayList<>();
        
        // Parse remaining tokens
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            
            // Check if it's a key=value parameter (but not an operator like ==, !=, >=, <=)
            if (token.contains("=") && !isOperator(token)) {
                String[] kv = token.split("=", 2);
                params.put(kv[0], kv[1]);
            } else {
                args.add(token);
            }
        }
        
        return new PipeCommandSpec(command, params, args);
    }
    
    /**
     * Check if token is an operator (not a parameter)
     */
    private static boolean isOperator(String token) {
        return token.equals("==") || token.equals("!=") || 
               token.equals(">=") || token.equals("<=") ||
               token.equals(">") || token.equals("<") || token.equals("=");
    }
    
    /**
     * Tokenize a command string respecting quotes
     */
    private static String[] tokenize(String str) {
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(str);
        while (m.find()) {
            String token = m.group(1);
            // Remove surrounding quotes if present
            if (token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1);
            }
            tokens.add(token);
        }
        return tokens.toArray(new String[0]);
    }
}
