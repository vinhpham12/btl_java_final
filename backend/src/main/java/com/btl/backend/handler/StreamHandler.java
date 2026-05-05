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
 */
public class StreamHandler implements HttpHandler {

    private final TracksDAO trackDao = new TracksDAO();

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

            byte[] audioData = FileStorageManager.readFile(filePath);

            exchange.getResponseHeaders().set("Content-Type", "audio/wav");
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(audioData.length));
            exchange.sendResponseHeaders(200, audioData.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(audioData);
            }
        } catch (NumberFormatException e) {
            HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Invalid track ID"));
        } catch (Exception e) {
            System.err.println("[StreamHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}
