/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.util;

/**
 *
 * @author ADMIN
 */
import java.util.*;

/**
 * Custom JSON parser and builder - no external libraries.
 * Supports: String, Number, Boolean, null, Object (Map), Array (List).
 */
public class JsonHelper {

    // ==================== JSON Builder ====================

    public static String toJson(Map<String, Object> map) {
        if (map == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            sb.append(valueToJson(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toJsonArray(List<Map<String, Object>> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escapeJson((String) value) + "\"";
        if (value instanceof Number) return value.toString();
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return toJson(map);
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(valueToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ==================== JSON Parser ====================

    public static Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        Parser parser = new Parser(json.trim());
        Object result = parser.parseValue();
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            return map;
        }
        return new HashMap<>();
    }

    public static List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        Parser parser = new Parser(json.trim());
        Object result = parser.parseValue();
        if (result instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) result;
            return list;
        }
        return new ArrayList<>();
    }

    public static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    public static String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    public static int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    public static long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    public static boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);
        return false;
    }

    // ==================== Simple JSON Response helpers ====================

    public static String successResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("message", message);
        return toJson(map);
    }

    public static String errorResponse(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "error");
        map.put("message", message);
        return toJson(map);
    }

    public static String dataResponse(Map<String, Object> data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("data", data);
        return toJson(map);
    }

    public static String listResponse(List<Map<String, Object>> data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "success");
        map.put("data", data);
        return toJson(map);
    }

    // ==================== Inner Parser Class ====================

    private static class Parser {
        private final String json;
        private int pos;

        Parser(String json) {
            this.json = json;
            this.pos = 0;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip '{'
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == '}') { pos++; return map; }

            while (pos < json.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else {
                    break;
                }
            }
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == '}') pos++;
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ']') { pos++; return list; }

            while (pos < json.length()) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                } else {
                    break;
                }
            }
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ']') pos++;
            return list;
        }

        String parseString() {
            if (json.charAt(pos) != '"') throw new RuntimeException("Expected '\"' at pos " + pos);
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == '\\') {
                    pos++;
                    if (pos < json.length()) {
                        char esc = json.charAt(pos);
                        switch (esc) {
                            case '"': sb.append('"'); break;
                            case '\\': sb.append('\\'); break;
                            case '/': sb.append('/'); break;
                            case 'n': sb.append('\n'); break;
                            case 'r': sb.append('\r'); break;
                            case 't': sb.append('\t'); break;
                            case 'u':
                                String hex = json.substring(pos + 1, pos + 5);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                                break;
                            default: sb.append(esc);
                        }
                    }
                } else if (c == '"') {
                    pos++;
                    return sb.toString();
                } else {
                    sb.append(c);
                }
                pos++;
            }
            return sb.toString();
        }

        Number parseNumber() {
            int start = pos;
            if (pos < json.length() && json.charAt(pos) == '-') pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
            boolean isDouble = false;
            if (pos < json.length() && json.charAt(pos) == '.') {
                isDouble = true;
                pos++;
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
            }
            if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) {
                isDouble = true;
                pos++;
                if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) pos++;
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
            }
            String numStr = json.substring(start, pos);
            if (isDouble) return Double.parseDouble(numStr);
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return (int) val;
            return val;
        }

        Boolean parseBoolean() {
            if (json.startsWith("true", pos)) { pos += 4; return true; }
            if (json.startsWith("false", pos)) { pos += 5; return false; }
            throw new RuntimeException("Invalid boolean at pos " + pos);
        }

        Object parseNull() {
            if (json.startsWith("null", pos)) { pos += 4; return null; }
            throw new RuntimeException("Invalid null at pos " + pos);
        }

        void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        }

        void expect(char c) {
            if (pos < json.length() && json.charAt(pos) == c) {
                pos++;
            } else {
                throw new RuntimeException("Expected '" + c + "' at pos " + pos);
            }
        }
    }
}
