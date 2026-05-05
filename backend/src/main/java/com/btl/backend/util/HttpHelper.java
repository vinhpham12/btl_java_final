/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.util;
//xu li giao thuc http 
/**
 *
 * @author ADMIN
 */
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class HttpHelper {
    //doc va phan tich du lieu dau vao
    public static String readRequestBody(HttpExchange exchange) throws IOException {//doc du lieu dang van ban
        try (InputStream is = exchange.getRequestBody();// mo luong inputstream de doc du lieu 
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
            return bos.toString(StandardCharsets.UTF_8);//ep thanh chuoi string bang ma utf-8
        }
    }

    public static byte[] readRequestBodyBytes(HttpExchange exchange) throws IOException {//doc du lieu dang nhi phan (gianh cho mp3 jpg
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
            return bos.toByteArray();//ep thanh chuoi byte
        }
    }
    
    //tra response cho frontend
    public static void sendJsonResponse(HttpExchange exchange, int code, String json) throws IOException {//tra kq ve chuoi JSon
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);//ep chuoi json thanh utf8
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {//ghi mang byte ra output stream
            os.write(bytes);
        }
    }

    public static void sendBinaryResponse(HttpExchange exchange, int code, byte[] data, String contentType) throws IOException {//tra ve ket qua dang file 
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, data.length);
        try (OutputStream os = exchange.getResponseBody()) {//ghi mang byte ra luong de tra ket qua
            os.write(data);
        }
    }
    //giai quyet loi cross-origin resource sharing (khi backend va frontend o 2 port khac nhau va muon lay du lieu cua thi van co the giao tiep
    public static void handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");//tra ve ma 204 va cac header cho phep ket noi 
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }
    
    //doc header Authorization tu frontend cat bo bearer de lay chuoi token tho sau do dung session manager giai ma token thanh id ng dung
    public static String getAuthToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
    public static int getAuthUserId(HttpExchange exchange) {
        String token = getAuthToken(exchange);
        return SessionManager.getUserId(token);
    }
    //boc tach duong dan url
    public static String extractPathParam(String path, String prefix) {//boc tach tham so dong 
        if (path.startsWith(prefix)) {
            String rest = path.substring(prefix.length());//cat bo phan prefix
            int slash = rest.indexOf('/');//tim den phan / de co lap va tim con so 
            return slash == -1 ? rest : rest.substring(0, slash);
        }
        return null;
    }
    
    public static Map<String, String> parseQueryParams(String query) {////boc tach cac tham so nam sau dau?(truy van db)
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {//cua doi chuoi o phan dau & 
            String[] kv = pair.split("=", 2);//sau do cua doi tiep sau dau =
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);//dung chuoi urldecode  giai ma va dua vao map
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    
    public static Map<String, Object> parseMultipart(HttpExchange exchange) throws IOException {
        Map<String, Object> result = new HashMap<>();
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("multipart/form-data")) return result;

        String boundary = contentType.substring(contentType.indexOf("boundary=") + 9).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }

        byte[] body = readRequestBodyBytes(exchange);
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        List<byte[]> parts = splitByBoundary(body, boundaryBytes);

        for (byte[] part : parts) {
            String partStr = new String(part, StandardCharsets.UTF_8);
            int headerEnd = partStr.indexOf("\r\n\r\n");
            if (headerEnd == -1) continue;

            String headers = partStr.substring(0, headerEnd);
            int dataStart = headerEnd + 4;

            String name = extractHeaderValue(headers, "name=\"", "\"");
            String filename = extractHeaderValue(headers, "filename=\"", "\"");

            if (filename != null && !filename.isEmpty()) {
                // File field - store as byte[]
                byte[] fileData = new byte[part.length - dataStart];
                System.arraycopy(part, dataStart, fileData, 0, fileData.length);
                // Remove trailing \r\n
                if (fileData.length >= 2 && fileData[fileData.length - 2] == '\r' && fileData[fileData.length - 1] == '\n') {
                    byte[] trimmed = new byte[fileData.length - 2];
                    System.arraycopy(fileData, 0, trimmed, 0, trimmed.length);
                    fileData = trimmed;
                }
                result.put(name, fileData);
                result.put(name + "_filename", filename);
            } else if (name != null) {
                // Text field
                String value = partStr.substring(dataStart).trim();
                result.put(name, value);
            }
        }
        return result;
    }

    private static List<byte[]> splitByBoundary(byte[] body, byte[] boundary) {
        List<byte[]> parts = new ArrayList<>();
        int start = indexOf(body, boundary, 0);
        if (start == -1) return parts;
        start += boundary.length;
        // skip CRLF after first boundary
        if (start < body.length - 1 && body[start] == '\r' && body[start + 1] == '\n') start += 2;

        while (true) {
            int end = indexOf(body, boundary, start);
            if (end == -1) break;
            byte[] part = new byte[end - start];
            System.arraycopy(body, start, part, 0, part.length);
            parts.add(part);
            start = end + boundary.length;
            if (start < body.length - 1 && body[start] == '-' && body[start + 1] == '-') break;
            if (start < body.length - 1 && body[start] == '\r' && body[start + 1] == '\n') start += 2;
        }
        return parts;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        for (int i = from; i <= data.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) { match = false; break; }
            }
            if (match) return i;
        }
        return -1;
    }

    private static String extractHeaderValue(String headers, String prefix, String suffix) {
        int start = headers.indexOf(prefix);
        if (start == -1) return null;
        start += prefix.length();
        int end = headers.indexOf(suffix, start);
        if (end == -1) return null;
        return headers.substring(start, end);
    }
}
