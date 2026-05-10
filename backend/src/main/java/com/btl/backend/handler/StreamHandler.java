/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.handler;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.DAO.TracksDAO;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;

/**
 * Streams audio file data for playback.
 * 
 * Tối ưu: Dùng buffered streaming thay vì đọc toàn bộ file vào RAM.
 * Với file 50MB, giảm từ 50MB RAM/request xuống chỉ 8KB (buffer).
 */
public class StreamHandler implements HttpHandler {

    private final TracksDAO trackDao = new TracksDAO();
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpHelper.sendJsonResponse(exchange, 405, JsonHelper.errorResponse("Method not allowed"));
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            // /api/tracks/{id}/stream
            String idStr = path.replace("/api/tracks/", "").replace("/stream", "");
            int trackId = Integer.parseInt(idStr);

            String filePath = trackDao.getFilePath(trackId);
            if (filePath == null || !FileStorageManager.fileExists(filePath)) {
                HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Audio file not found"));
                return;
            }

            File audioFile = new File(filePath);
            long fileLength = audioFile.length();

            exchange.getResponseHeaders().set("Content-Type", "audio/wav");
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileLength));
            exchange.sendResponseHeaders(200, fileLength);

            // Streaming: đọc và gửi từng chunk 8KB thay vì load toàn bộ vào RAM
            try (FileInputStream fis = new FileInputStream(audioFile);
                 BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (NumberFormatException e) {
            HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Invalid track ID"));
        } catch (Exception e) {
            System.err.println("[StreamHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}

