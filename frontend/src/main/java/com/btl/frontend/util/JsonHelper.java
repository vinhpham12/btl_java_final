/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.frontend.util;

/**
 *
 * @author ADMIN
 */
import java.util.*;

/**
 * Custom JSON parser/builder for frontend (same logic as backend).
 */
public class JsonHelper {

    public static String toJson(Map<String, Object> map) {
        if (map == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            sb.append(valueToJson(entry.getValue()));
            first = false;
        }
        return sb.append("}").toString();
    }

    private static String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escapeJson((String) value) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) { @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) value; return toJson(m); }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public static Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        return new Parser(json.trim()).parseObject();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        Object result = new Parser(json.trim()).parseValue();
        if (result instanceof List) return (List<Map<String, Object>>) result;
        return new ArrayList<>();
    }

    public static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    public static String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    public static int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) try { return Integer.parseInt((String) val); } catch (Exception e) {}
        return 0;
    }

    public static boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map) return (Map<String, Object>) val;
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) return (List<Map<String, Object>>) val;
        return new ArrayList<>();
    }

    // Parser
    private static class Parser {
        private final String json;
        private int pos;

        Parser(String json) { this.json = json; this.pos = 0; }

        Object parseValue() {
            skipWS();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') { pos += 4; return null; }
            return parseNumber();
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWS();
            if (pos < json.length() && json.charAt(pos) == '}') { pos++; return map; }
            while (pos < json.length()) {
                skipWS(); String key = parseString(); skipWS(); pos++; skipWS();
                map.put(key, parseValue());
                skipWS();
                if (pos < json.length() && json.charAt(pos) == ',') pos++; else break;
            }
            skipWS();
            if (pos < json.length() && json.charAt(pos) == '}') pos++;
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWS();
            if (pos < json.length() && json.charAt(pos) == ']') { pos++; return list; }
            while (pos < json.length()) {
                skipWS(); list.add(parseValue()); skipWS();
                if (pos < json.length() && json.charAt(pos) == ',') pos++; else break;
            }
            skipWS();
            if (pos < json.length() && json.charAt(pos) == ']') pos++;
            return list;
        }

        String parseString() {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == '\\') { pos++; char e = json.charAt(pos);
                    switch(e) { case '"': sb.append('"'); break; case '\\': sb.append('\\'); break;
                        case 'n': sb.append('\n'); break; case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break; case 'u': sb.append((char)Integer.parseInt(json.substring(pos+1,pos+5),16)); pos+=4; break;
                        default: sb.append(e); }
                } else if (c == '"') { pos++; return sb.toString(); } else sb.append(c);
                pos++;
            }
            return sb.toString();
        }

        Number parseNumber() {
            int start = pos;
            if (pos < json.length() && json.charAt(pos) == '-') pos++;
            while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
            boolean d = false;
            if (pos < json.length() && json.charAt(pos) == '.') { d = true; pos++; while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++; }
            String s = json.substring(start, pos);
            if (d) return Double.parseDouble(s);
            long v = Long.parseLong(s);
            return (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) ? (int) v : v;
        }

        Boolean parseBool() {
            if (json.startsWith("true", pos)) { pos += 4; return true; }
            pos += 5; return false;
        }

        void skipWS() { while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++; }
    }
}
