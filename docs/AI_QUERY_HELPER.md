# AI Query Helper

The AI Query Helper uses Spring AI with Ollama to help you write log search queries using natural language.

## Setup

### 1. Install Ollama

```bash
brew install ollama
```

### 2. Pull a Model

```bash
# Recommended: Llama 3.2 (configured by default)
ollama pull llama3.2

# Alternative models:
ollama pull llama2
ollama pull codellama
```

### 3. Start Ollama Service

```bash
ollama serve
```

This will start the Ollama service on `http://localhost:11434`.

## Usage

1. Click the **🤖 AI Help** button next to the query input
2. Describe what you want to search for in natural language
3. The AI will suggest a query using Lucene syntax and pipe commands
4. Click **Use This Query** to apply the suggestion

## Examples

**User Request**: "Show me all errors in the last hour grouped by user"
```
level:ERROR | stats count by user
```

**User Request**: "Find slow queries and calculate average duration"
```
status:slow | stats avg(duration) by operation
```

**User Request**: "Add a filter to only show amounts greater than 100"
```
* | filter amount > 100
```

**User Request**: "Show me database errors as a pie chart by component"
```
level:ERROR AND component:database | chart type=pie count by component
```

## Configuration

Edit `application.properties` to change the model or Ollama URL:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.chat.options.temperature=0.7
```

## Troubleshooting

**Error: AI service unavailable**
- Make sure Ollama is running: `ollama serve`
- Check the service is accessible: `curl http://localhost:11434/api/tags`
- Verify the model is downloaded: `ollama list`

**Slow responses**
- The first request may be slow as the model loads into memory
- Subsequent requests should be faster
- Consider using a smaller model like `llama2` if performance is an issue

**Wrong suggestions**
- Try being more specific in your request
- Include examples of field names in your data
- Mention specific pipe commands you want to use
