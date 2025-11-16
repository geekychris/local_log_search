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
