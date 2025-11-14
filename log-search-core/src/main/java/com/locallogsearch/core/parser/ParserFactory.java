package com.locallogsearch.core.parser;

import java.util.Map;

public class ParserFactory {
    
    public static LogParser createParser(String parserType, Map<String, String> config) {
        LogParser parser;
        
        switch (parserType.toLowerCase()) {
            case "keyvalue":
                parser = new KeyValueParser();
                break;
            case "regex":
                parser = new RegexParser();
                break;
            case "grok":
                parser = new GrokParser();
                break;
            case "custom":
                // Load custom parser class
                String className = config.get("class");
                if (className == null) {
                    throw new IllegalArgumentException("Custom parser requires 'class' configuration");
                }
                try {
                    Class<?> clazz = Class.forName(className);
                    parser = (LogParser) clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate custom parser: " + className, e);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown parser type: " + parserType);
        }
        
        parser.configure(config);
        return parser;
    }
}
