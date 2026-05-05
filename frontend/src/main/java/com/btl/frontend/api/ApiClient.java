/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.frontend.api;

/**
 *
 * @author ADMIN
 */
import com.btl.frontend.util.JsonHelper;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP Client gọi API backend.
 *
 * Sử dụng java.net.HttpURLConnection (tích hợp trong JDK, không cần thư viện ngoài).
 *
 * Chức năng:
 * - get/post/put/delete: Gọi REST API với JSON body
 * - uploadMultipart: Upload file (multipart/form-data)
 * - downloadBytes: Tải dữ liệu binary (stream nhạc)
 *
 * Tự động đính kèm token xác thực vào mỗi request nếu đã đăng nhập.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8081/api"; // Địa chỉ backend
    private static String authToken = null;  // Token đăng nhập (null = chưa đăng nhập)

    // === Quản lý token ===
    public static void setAuthToken(String token) { authToken = token; }
    public static String getAuthToken() { return authToken; }
    public static boolean isAuthenticated() { return authToken != null; }

    // ========== GET - Lấy dữ liệu ==========
    public static Map<String, Object> get(String path) throws IOException {
        HttpURLConnection conn = openConnection(path, "GET");
        return readJsonResponse(conn);
    }

    // ========== POST - Gửi dữ liệu JSON ==========
    public static Map<String, Object> post(String path, Map<String, Object> body) throws IOException {
        HttpURLConnection conn = openConnection(path, "POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(JsonHelper.toJson(body).getBytes(StandardCharsets.UTF_8));
        }
        return readJsonResponse(conn);
    }

    // ========== PUT ==========
    public static Map<String, Object> put(String path, Map<String, Object> body) throws IOException {
        HttpURLConnection conn = openConnection(path, "PUT");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(JsonHelper.toJson(body).getBytes(StandardCharsets.UTF_8));
        }
        return readJsonResponse(conn);
    }

    // ========== DELETE ==========
    public static Map<String, Object> delete(String path) throws IOException {
        HttpURLConnection conn = openConnection(path, "DELETE");
        return readJsonResponse(conn);
    }

    /**
     * Upload file bằng multipart/form-data.
     *
     * Multipart là gì?
     * - Format cho phép gửi cả text và binary trong 1 request
     * - Mỗi phần được ngăn cách bởi "boundary" (chuỗi ngẫu nhiên)
     * - Dùng khi upload file nhạc kèm metadata (title, artist, genre)
     *
     * Cấu trúc:
     * --boundary
     * Content-Disposition: form-data; name="title"
     * My Song
     * --boundary
     * Content-Disposition: form-data; name="file"; filename="song.wav"
     * [binary data]
     * --boundary--
     */
    public static Map<String, Object> uploadMultipart(String path, Map<String, String> fields, String fileFieldName, byte[] fileData, String filename) throws IOException {
        String boundary = "----Boundary" + System.currentTimeMillis();
        HttpURLConnection conn = openConnection(path, "POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            // Text fields
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes());
                os.write((entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            // File field
            if (fileData != null) {
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + filename + "\"\r\n").getBytes());
                os.write("Content-Type: application/octet-stream\r\n\r\n".getBytes());
                os.write(fileData);
                os.write("\r\n".getBytes());
            }
            os.write(("--" + boundary + "--\r\n").getBytes());
        }
        return readJsonResponse(conn);
    }

    // ========== Tải dữ liệu binary (stream nhạc) ==========
    public static byte[] downloadBytes(String path) throws IOException {
        HttpURLConnection conn = openConnection(path, "GET");
        int code = conn.getResponseCode();
        if (code != 200) return null;
        try (InputStream is = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
            return bos.toByteArray();
        }
    }

    // ========== Hàm nội bộ ==========

    /**
     * Tạo kết nối HTTP.
     * Tự động thêm header Authorization nếu đã có token.
     */
    private static HttpURLConnection openConnection(String path, String method) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);  // Timeout kết nối: 10s
        conn.setReadTimeout(30000);     // Timeout đọc: 30s (file nhạc lớn)
        if (authToken != null) {
            // Gửi token trong header: Authorization: Bearer <token>
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        return conn;
    }

    /** Đọc response JSON từ server. Xử lý cả response thành công và lỗi. */
    private static Map<String, Object> readJsonResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        // 2xx/3xx dùng getInputStream(), 4xx/5xx dùng getErrorStream()
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "No response");
            return err;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return JsonHelper.parseJson(sb.toString());
        }
    }
}