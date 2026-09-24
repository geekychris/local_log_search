/*
 * MIT License — same terms as the parent project.
 */

package com.locallogsearch.core.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallogsearch.core.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Map;

/**
 * Parses JSON-lines log format — one JSON object per line, keys become fields.
 *
 * <p>Motivated by Go's {@code log/slog.NewJSONHandler} and its equivalents in
 * other languages (Python's {@code structlog}, Node's {@code pino}, etc.),
 * which emit lines like:
 *
 * <pre>{@code {"time":"2026-09-23T02:02:00.882Z","level":"INFO","msg":"chiefd started","pid":16625}}</pre>
 *
 * <p>Behavior:
 * <ul>
 *   <li>Every top-level key becomes a field via {@link LogEntry#addField}.
 *       Nested objects/arrays are stored as their JSON string form so the
 *       original data is searchable but structure is preserved.</li>
 *   <li>Timestamp is extracted from the first key that parses as an ISO-8601
 *       instant. Configurable via {@code timestamp_field} (default "time",
 *       falls back to "timestamp" then "ts").</li>
 *   <li>Level is copied to a canonical "level" field for the UI facets.
 *       Configurable via {@code level_field} (default "level").</li>
 *   <li>Non-JSON lines (blank or malformed) are silently skipped — the tailer
 *       calls parse() on every line and we don't want a mid-stream comment
 *       or a partial write to blow up ingestion.</li>
 * </ul>
 *
 * <p>Config keys (all optional):
 * <ul>
 *   <li>{@code timestamp_field} — key to look up first for the timestamp</li>
 *   <li>{@code level_field} — key to normalize into "level"</li>
 * </ul>
 */
public class JsonParser implements LogParser {

    private static final Logger log = LoggerFactory.getLogger(JsonParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Ordered fallbacks so common structured-logger conventions all work
    // without config: slog ("time"), structlog/logback ("timestamp"),
    // pino/zap ("ts").
    private static final String[] DEFAULT_TIMESTAMP_KEYS = {"time", "timestamp", "ts", "@timestamp"};
    private static final String[] DEFAULT_LEVEL_KEYS = {"level", "severity", "lvl"};

    private String timestampField;
    private String levelField;

    @Override
    public void configure(Map<String, String> config) {
        if (config == null) {
            return;
        }
        this.timestampField = config.get("timestamp_field");
        this.levelField = config.get("level_field");
    }

    @Override
    public void parse(LogEntry entry) {
        String text = entry.getRawText();
        if (text == null || text.isEmpty()) {
            return;
        }
        String trimmed = text.trim();
        // Fast reject: only parse things that look like a JSON object.
        // Skips comment lines, partial writes, and anything else non-JSON
        // without spinning up Jackson.
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return;
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(trimmed);
        } catch (JsonProcessingException e) {
            // Malformed / truncated line — skip. Debug-level only so a
            // busy log with occasional partial writes doesn't flood our
            // own warning stream.
            log.debug("skipping non-JSON line ({}): {}", e.getOriginalMessage(),
                    trimmed.length() > 120 ? trimmed.substring(0, 117) + "..." : trimmed);
            return;
        }
        if (!root.isObject()) {
            return;
        }

        // Emit every top-level key as a field. Nested containers are
        // serialized back to JSON strings — searchable, and callers can
        // re-parse if they want structure.
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> f = fields.next();
            String key = f.getKey();
            JsonNode value = f.getValue();
            String stringValue;
            if (value.isTextual()) {
                stringValue = value.asText();
            } else if (value.isValueNode()) {
                stringValue = value.asText();
            } else {
                stringValue = value.toString();
            }
            entry.addField(key, stringValue);
        }

        // Canonicalize the level so the UI's level facet works regardless
        // of the emitter's key convention.
        String level = firstNonEmpty(root,
                levelField != null ? new String[]{levelField} : DEFAULT_LEVEL_KEYS);
        if (level != null) {
            entry.addField("level", level);
        }

        // Try to lift a timestamp so time-range filters and sort-by-time work.
        String[] tsKeys = timestampField != null
                ? new String[]{timestampField}
                : DEFAULT_TIMESTAMP_KEYS;
        for (String key : tsKeys) {
            JsonNode node = root.get(key);
            if (node == null || !node.isValueNode() || node.isNull()) {
                continue;
            }
            String s = node.asText();
            Instant t = parseInstantLenient(s);
            if (t != null) {
                entry.setTimestamp(t);
                break;
            }
        }
    }

    private static String firstNonEmpty(JsonNode root, String[] keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && node.isValueNode() && !node.isNull()) {
                String s = node.asText();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Lenient ISO-8601 parse. slog emits e.g. "2026-09-23T02:02:00.882Z"
     * which {@link Instant#parse} handles directly; other loggers may
     * omit the fractional seconds or use offsets.
     */
    private static Instant parseInstantLenient(String s) {
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ignored) {
            // Fallback: some loggers emit epoch millis as a number.
            try {
                long millis = Long.parseLong(s);
                return Instant.ofEpochMilli(millis);
            } catch (NumberFormatException ignored2) {
                return null;
            }
        }
    }
}
