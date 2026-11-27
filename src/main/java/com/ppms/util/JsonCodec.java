// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Zero-dependency RFC-8259 compliant JSON parser and serializer implemented in pure Core Java.
 */
public final class JsonCodec {

    private JsonCodec() {}

    // --- Serialization ---

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
            if (value instanceof Double d && (d.isNaN() || d.isInfinite())) {
                sb.append("null");
            } else if (value instanceof BigDecimal bd) {
                sb.append(bd.toPlainString());
            } else {
                sb.append(value);
            }
        } else if (value instanceof Number n) {
            sb.append(n);
        } else if (value instanceof CharSequence cs) {
            writeString(sb, cs.toString());
        } else if (value instanceof Enum<?> e) {
            writeString(sb, e.name());
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
        } else if (value instanceof Iterable<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
        } else if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            sb.append('[');
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                writeValue(sb, java.lang.reflect.Array.get(value, i));
            }
            sb.append(']');
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // --- Parsing ---

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Object parsed = new Parser(json).parseRoot();
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Expected a JSON object.");
    }

    public static Object parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON payload cannot be empty.");
        }
        return new Parser(json).parseRoot();
    }

    public static String getString(Map<String, Object> obj, String key) {
        Object val = obj.get(key);
        return val == null ? null : String.valueOf(val);
    }

    public static String getStringOrDefault(Map<String, Object> obj, String key, String defaultVal) {
        Object val = obj.get(key);
        return val == null ? defaultVal : String.valueOf(val);
    }

    public static BigDecimal getBigDecimal(Map<String, Object> obj, String key) {
        Object val = obj.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof BigDecimal bd) {
            return bd;
        }
        if (val instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        String raw = String.valueOf(val).trim();
        if (raw.isEmpty()) {
            return null;
        }
        return CurrencyFormatter.parse(raw);
    }

    public static Integer getInt(Map<String, Object> obj, String key) {
        Object val = obj.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof Number n) {
            BigDecimal bd = new BigDecimal(n.toString());
            return bd.intValueExact();
        }
        String raw = String.valueOf(val).trim();
        if (raw.isEmpty()) {
            return null;
        }
        return Integer.parseInt(raw);
    }

    public static Boolean getBoolean(Map<String, Object> obj, String key) {
        Object val = obj.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(val).trim());
    }

    private static final class Parser {
        private final String src;
        private int pos = 0;

        Parser(String src) {
            this.src = src;
        }

        Object parseRoot() {
            skipWhitespace();
            Object val = parseValue();
            skipWhitespace();
            if (pos < src.length()) {
                throw new IllegalArgumentException("Unexpected trailing character in JSON at position " + pos);
            }
            return val;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON input.");
            }
            char c = src.charAt(pos);
            return switch (c) {
                case '{' -> parseObjectInternal();
                case '[' -> parseArrayInternal();
                case '"' -> parseStringInternal();
                case 't', 'f' -> parseBooleanInternal();
                case 'n' -> parseNullInternal();
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        yield parseNumberInternal();
                    }
                    throw new IllegalArgumentException("Invalid JSON character '" + c + "' at position " + pos);
                }
            };
        }

        private Map<String, Object> parseObjectInternal() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) {
                    throw new IllegalArgumentException("Expected JSON string key at position " + pos);
                }
                String key = parseStringInternal();
                skipWhitespace();
                expect(':');
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (peek('}')) {
                    pos++;
                    break;
                }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArrayInternal() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    pos++;
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseStringInternal() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= src.length()) {
                        throw new IllegalArgumentException("Unterminated escape sequence in JSON string.");
                    }
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape in JSON.");
                            }
                            String hex = src.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new IllegalArgumentException("Invalid JSON escape \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private Object parseBooleanInternal() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean literal at position " + pos);
        }

        private Object parseNullInternal() {
            if (src.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null literal at position " + pos);
        }

        private BigDecimal parseNumberInternal() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            if (pos < src.length() && src.charAt(pos) == '.') {
                pos++;
                if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                    throw new IllegalArgumentException("Invalid fractional number at position " + pos);
                }
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
                if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                    throw new IllegalArgumentException("Invalid exponent at position " + pos);
                }
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            return new BigDecimal(src.substring(start, pos));
        }

        private void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private boolean peek(char expected) {
            return pos < src.length() && src.charAt(pos) == expected;
        }

        private void expect(char expected) {
            if (pos >= src.length() || src.charAt(pos) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos);
            }
            pos++;
        }
    }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
