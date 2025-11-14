# Grok Patterns Reference

## What is Grok?

Grok is a powerful pattern matching tool that uses predefined patterns to parse log entries. It's especially useful for parsing common log formats like Apache, Nginx, syslog, etc.

---

## Common Grok Patterns

### Apache/Nginx Access Logs

**Pattern Name:** `COMBINEDAPACHELOG`

**Example Log:**
```
127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326 "http://www.example.com/start.html" "Mozilla/4.08 [en] (Win98; I ;Nav)"
```

**Parser Configuration:**
```json
{
  "pattern": "%{COMBINEDAPACHELOG}"
}
```

**Extracted Fields:**
- `clientip`: 127.0.0.1
- `ident`: -
- `auth`: frank
- `timestamp`: 10/Oct/2000:13:55:36 -0700
- `verb`: GET
- `request`: /apache_pb.gif
- `httpversion`: 1.0
- `response`: 200
- `bytes`: 2326
- `referrer`: http://www.example.com/start.html
- `agent`: Mozilla/4.08 [en] (Win98; I ;Nav)

---

### Apache Common Log Format

**Pattern Name:** `COMMONAPACHELOG`

**Example Log:**
```
127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326
```

**Parser Configuration:**
```json
{
  "pattern": "%{COMMONAPACHELOG}"
}
```

**Extracted Fields:**
- `clientip`: 127.0.0.1
- `ident`: -
- `auth`: frank
- `timestamp`: 10/Oct/2000:13:55:36 -0700
- `verb`: GET
- `request`: /apache_pb.gif
- `httpversion`: 1.0
- `response`: 200
- `bytes`: 2326

---

### Syslog

**Pattern Name:** `SYSLOGBASE`

**Example Log:**
```
Mar 16 00:01:25 hostname sshd[1234]: Failed password for invalid user admin from 192.168.1.100 port 22 ssh2
```

**Parser Configuration:**
```json
{
  "pattern": "%{SYSLOGBASE} %{GREEDYDATA:message}"
}
```

**Extracted Fields:**
- `timestamp`: Mar 16 00:01:25
- `logsource`: hostname
- `program`: sshd
- `pid`: 1234
- `message`: Failed password for invalid user admin from 192.168.1.100 port 22 ssh2

---

### Java Stack Traces

**Pattern Name:** `JAVACLASS` and `JAVALOGMESSAGE`

**Example Log:**
```
2024-01-15 10:30:45,123 ERROR [main] com.example.MyClass - Database connection failed
java.sql.SQLException: Connection timeout
    at com.mysql.jdbc.ConnectionImpl.connect(ConnectionImpl.java:123)
    at com.example.MyClass.main(MyClass.java:45)
```

**Parser Configuration:**
```json
{
  "pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \\[%{DATA:thread}\\] %{JAVACLASS:class} - %{GREEDYDATA:message}"
}
```

**Extracted Fields:**
- `timestamp`: 2024-01-15 10:30:45,123
- `level`: ERROR
- `thread`: main
- `class`: com.example.MyClass
- `message`: Database connection failed...

---

### Custom Application Logs

#### Example 1: Structured Application Log

**Example Log:**
```
[2024-01-15 10:30:00] INFO [UserService] User login attempt: user=john.doe ip=192.168.1.50 success=true
```

**Parser Configuration:**
```json
{
  "pattern": "\\[%{TIMESTAMP_ISO8601:timestamp}\\] %{LOGLEVEL:level} \\[%{DATA:service}\\] %{DATA:action}: user=%{USERNAME:user} ip=%{IP:ip} success=%{WORD:success}"
}
```

**Extracted Fields:**
- `timestamp`: 2024-01-15 10:30:00
- `level`: INFO
- `service`: UserService
- `action`: User login attempt
- `user`: john.doe
- `ip`: 192.168.1.50
- `success`: true

#### Example 2: JSON-like Logs

**Example Log:**
```
2024-01-15T10:30:00.123Z [payment-service] Processing payment transaction_id=txn_12345 amount=99.99 currency=USD status=completed
```

**Parser Configuration:**
```json
{
  "pattern": "%{TIMESTAMP_ISO8601:timestamp} \\[%{DATA:service}\\] %{DATA:action} transaction_id=%{WORD:transactionId} amount=%{NUMBER:amount} currency=%{WORD:currency} status=%{WORD:status}"
}
```

**Extracted Fields:**
- `timestamp`: 2024-01-15T10:30:00.123Z
- `service`: payment-service
- `action`: Processing payment
- `transactionId`: txn_12345
- `amount`: 99.99
- `currency`: USD
- `status`: completed

---

## Common Grok Pattern Components

### Basic Patterns

| Pattern | Matches | Example |
|---------|---------|---------|
| `%{NUMBER}` | Any number | 123, 45.67, -89 |
| `%{INT}` | Integer | 123, -456 |
| `%{WORD}` | Single word | hello, user123 |
| `%{DATA}` | Any characters (non-greedy) | hello world |
| `%{GREEDYDATA}` | Any characters (greedy) | everything to end of line |
| `%{SPACE}` | Whitespace | (spaces/tabs) |
| `%{NOTSPACE}` | Non-whitespace | word |
| `%{QUOTEDSTRING}` | Quoted text | "hello world" |

### Network Patterns

| Pattern | Matches | Example |
|---------|---------|---------|
| `%{IP}` | IP address (v4 or v6) | 192.168.1.1 |
| `%{IPV4}` | IPv4 address | 192.168.1.1 |
| `%{IPV6}` | IPv6 address | 2001:db8::1 |
| `%{HOSTNAME}` | Hostname | server.example.com |
| `%{MAC}` | MAC address | 00:1B:63:84:45:E6 |
| `%{URI}` | URI/URL | http://example.com/path |

### Date/Time Patterns

| Pattern | Matches | Example |
|---------|---------|---------|
| `%{TIMESTAMP_ISO8601}` | ISO 8601 timestamp | 2024-01-15T10:30:00.123Z |
| `%{DATE}` | Date (various formats) | 01/15/2024 |
| `%{TIME}` | Time | 10:30:45 |
| `%{YEAR}` | Year | 2024 |
| `%{MONTH}` | Month name | January, Jan |
| `%{MONTHNUM}` | Month number | 01, 12 |
| `%{DAY}` | Day name | Monday, Mon |

### Application Patterns

| Pattern | Matches | Example |
|---------|---------|---------|
| `%{LOGLEVEL}` | Log level | DEBUG, INFO, WARN, ERROR |
| `%{USERNAME}` | Username | john.doe, user_123 |
| `%{EMAILADDRESS}` | Email | user@example.com |
| `%{PATH}` | File path | /var/log/app.log |
| `%{UUID}` | UUID | 550e8400-e29b-41d4-a716-446655440000 |
| `%{JAVACLASS}` | Java class name | com.example.MyClass |

---

## Using Grok in the UI

### Step 1: Choose Parser Type
In the log sources UI, select **"Grok Pattern"** as the parser type.

### Step 2: Enter Configuration
In the Parser Configuration field, enter a JSON object with the pattern:
```json
{
  "pattern": "%{COMBINEDAPACHELOG}"
}
```

### Step 3: Test with Sample
Use the "Load Preview" feature to test your pattern:
1. Enter or browse to your log file
2. Click "Load Preview"
3. Select sample log lines
4. See how the pattern extracts fields

### Step 4: Refine as Needed
If the pattern doesn't match:
- Check the pattern syntax
- Try a more specific pattern
- Add custom regex if needed

---

## Advanced Techniques

### Named Captures

You can give custom names to extracted fields:

```json
{
  "pattern": "%{IP:client_ip} - %{USER:username} \\[%{HTTPDATE:timestamp}\\]"
}
```

This extracts the IP as `client_ip` instead of just `ip`.

### Combining Patterns

You can combine multiple patterns:

```json
{
  "pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \\[%{DATA:service}\\] %{GREEDYDATA:message}"
}
```

### Custom Patterns

If standard patterns don't work, you can use raw regex:

```json
{
  "pattern": "%{TIMESTAMP_ISO8601:timestamp} (?<custom_field>\\w+) %{GREEDYDATA:message}"
}
```

The `(?<name>regex)` syntax creates named capture groups.

---

## Complete Examples for Different Log Types

### 1. Nginx Access Log

**Log:**
```
192.168.1.100 - - [15/Jan/2024:10:30:45 +0000] "GET /api/users HTTP/1.1" 200 1234 "-" "Mozilla/5.0"
```

**Config:**
```json
{
  "pattern": "%{COMBINEDAPACHELOG}"
}
```

### 2. AWS ELB Log

**Log:**
```
2024-01-15T10:30:45.123456Z app/my-loadbalancer/50dc6c495c0c9188 192.168.1.1:48724 10.0.0.1:80 0.001 0.002 0.000 200 200 0 257 "GET http://example.com:80/ HTTP/1.1"
```

**Config:**
```json
{
  "pattern": "%{TIMESTAMP_ISO8601:timestamp} %{NOTSPACE:elb} %{IP:client_ip}:%{NUMBER:client_port} %{IP:backend_ip}:%{NUMBER:backend_port} %{NUMBER:request_processing_time} %{NUMBER:backend_processing_time} %{NUMBER:response_processing_time} %{NUMBER:elb_status_code} %{NUMBER:backend_status_code} %{NUMBER:received_bytes} %{NUMBER:sent_bytes} \"%{DATA:request}\""
}
```

### 3. MySQL Slow Query Log

**Log:**
```
# Time: 2024-01-15T10:30:45.123456Z
# User@Host: app_user[app_user] @ localhost [127.0.0.1]
# Query_time: 2.345678  Lock_time: 0.000123 Rows_sent: 100  Rows_examined: 10000
SELECT * FROM users WHERE status = 'active';
```

**Config:**
```json
{
  "pattern": "# Time: %{TIMESTAMP_ISO8601:timestamp}\\n# User@Host: %{USER:user}\\[%{USER:db_user}\\] @ %{HOSTNAME:host} \\[%{IP:ip}\\]\\n# Query_time: %{NUMBER:query_time}  Lock_time: %{NUMBER:lock_time} Rows_sent: %{NUMBER:rows_sent}  Rows_examined: %{NUMBER:rows_examined}\\n%{GREEDYDATA:query}"
}
```

### 4. Docker Container Log

**Log:**
```
2024-01-15T10:30:45.123456789Z container_name stdout F {"level":"info","msg":"Request processed","duration":125,"status":200}
```

**Config:**
```json
{
  "pattern": "%{TIMESTAMP_ISO8601:timestamp} %{NOTSPACE:container_name} %{WORD:stream} %{WORD:log_type} %{GREEDYDATA:message}"
}
```

### 5. HAProxy Log

**Log:**
```
Jan 15 10:30:45 localhost haproxy[12345]: 192.168.1.100:54321 [15/Jan/2024:10:30:45.123] frontend_name backend_name/server1 0/0/1/2/3 200 1234 - - ---- 1/1/0/0/0 0/0 "GET /api/users HTTP/1.1"
```

**Config:**
```json
{
  "pattern": "%{SYSLOGTIMESTAMP:syslog_timestamp} %{SYSLOGHOST:syslog_host} %{SYSLOGPROG}: %{IP:client_ip}:%{INT:client_port} \\[%{HAPROXYDATE:timestamp}\\] %{NOTSPACE:frontend_name} %{NOTSPACE:backend_name}/%{NOTSPACE:server_name} %{INT:time_request}/%{INT:time_queue}/%{INT:time_backend_connect}/%{INT:time_backend_response}/%{NOTSPACE:time_duration} %{INT:status_code} %{NOTSPACE:bytes_read} %{DATA:captured_request_cookie} %{DATA:captured_response_cookie} %{NOTSPACE:termination_state} %{INT:actconn}/%{INT:feconn}/%{INT:beconn}/%{INT:srv_conn}/%{NOTSPACE:retries} %{INT:srv_queue}/%{INT:backend_queue} \"%{GREEDYDATA:http_request}\""
}
```

---

## Tips for Creating Grok Patterns

### 1. Start Simple
Begin with basic patterns and add complexity:
```json
// Start with
{"pattern": "%{TIMESTAMP_ISO8601:timestamp} %{GREEDYDATA:message}"}

// Then refine
{"pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}"}
```

### 2. Test with Real Data
Always test your patterns with actual log samples in the UI preview.

### 3. Use GREEDYDATA Wisely
Place `%{GREEDYDATA}` at the end of your pattern - it consumes everything remaining.

### 4. Escape Special Characters
Remember to escape special regex characters:
- `[` → `\\[`
- `]` → `\\]`
- `(` → `\\(`
- `)` → `\\)`
- `.` → `\\.`

### 5. Common Mistakes
- Forgetting to escape brackets: `[timestamp]` → `\\[%{DATA:timestamp}\\]`
- Using GREEDYDATA too early: `%{GREEDYDATA} %{NUMBER}` won't work
- Not matching exact spacing: `user:john` vs `user: john`

---

## Testing Your Patterns

### Online Grok Debugger
You can test patterns at: https://grokdebugger.com/ (or similar tools)

### In the UI
1. Go to Log Sources page
2. Click "Add New Log Source" or "Edit" on existing source
3. Select parser type "Grok Pattern"
4. Enter your configuration
5. Click "Load Preview" on a log file
6. Select sample lines
7. See parsed output immediately

---

## Reference Links

- **Grok Pattern Repository:** https://github.com/logstash-plugins/logstash-patterns-core/tree/main/patterns
- **Online Debugger:** https://grokdebugger.com/
- **Regex Reference:** https://regex101.com/

---

## Quick Reference

### Most Common Patterns
```
%{COMBINEDAPACHELOG}        # Apache/Nginx combined logs
%{COMMONAPACHELOG}          # Apache common logs  
%{SYSLOGBASE}               # Syslog format
%{TIMESTAMP_ISO8601}        # ISO timestamps
%{IP}                       # IP addresses
%{LOGLEVEL}                 # Log levels
%{NUMBER}                   # Numbers
%{WORD}                     # Single words
%{GREEDYDATA}               # Everything else
```

### Basic Configuration Template
```json
{
  "pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}"
}
```
