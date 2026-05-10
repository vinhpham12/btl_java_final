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
import com.btl.backend.model.Tracks;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TrackHandler implements HttpHandler {

    private final TracksDAO trackDao = new TracksDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method) && path.equals("/api/tracks/trending")) {
                handleTrending(exchange);
            } else if ("GET".equals(method) && path.equals("/api/tracks")) {
                handleList(exchange);
            } else if ("GET".equals(method) && path.matches("/api/tracks/\\d+")) {
                handleGetById(exchange, path);
            } else if ("POST".equals(method) && path.equals("/api/tracks")) {
                handleUpload(exchange);
            } else if ("DELETE".equals(method) && path.matches("/api/tracks/\\d+")) {
                handleDelete(exchange, path);
            } else if ("POST".equals(method) && path.matches("/api/tracks/\\d+/play")) {
                handlePlay(exchange, path);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Not found"));
            }
        } catch (Exception e) {
            System.err.println("[TrackHandler] Error: " + e.getMessage());
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        int userId = HttpHelper.getAuthUserId(exchange);
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        int offset = 0, limit = 20;
        String sort = params.getOrDefault("sort", "newest");
        try { offset = Integer.parseInt(params.getOrDefault("offset", "0")); } catch (Exception e) {}
        try { limit = Integer.parseInt(params.getOrDefault("limit", "20")); } catch (Exception e) {}

        List<Tracks> tracks = trackDao.findAll(userId, offset, limit, sort);
        List<Map<String, Object>> jsonList = tracks.stream().map(Tracks::toJson).collect(Collectors.toList());
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.listResponse(jsonList));
    }

    private void handleGetById(HttpExchange exchange, String path) throws IOException {
        int userId = HttpHelper.getAuthUserId(exchange);
        int trackId = extractId(path, "/api/tracks/");
        Tracks track = trackDao.findById(trackId, userId);
        if (track == null) {
            HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Track not found"));
            return;
        }
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(track.toJson()));
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
        int userId = HttpHelper.getAuthUserId(exchange);
        if (userId == -1) {
            HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
            return;
        }

        Map<String, Object> parts = HttpHelper.parseMultipart(exchange);

        String title = (String) parts.getOrDefault("title", "Untitled");
        String artist = (String) parts.getOrDefault("artist", "Unknown");
        String genre = (String) parts.getOrDefault("genre", "");
        String description = (String) parts.getOrDefault("description", "");

        byte[] audioData = (byte[]) parts.get("file");
        if (audioData == null) {
            HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("No audio file provided"));
            return;
        }

        String audioFilename = (String) parts.getOrDefault("file_filename", "track.wav");
        String filePath = FileStorageManager.saveTrack(audioData, audioFilename);

        // Calculate duration from WAV header
        int durationSeconds = calculateWavDuration(audioData);

        // Save cover art if provided
        String coverPath = null;
        byte[] coverData = (byte[]) parts.get("cover");
        if (coverData != null) {
            String coverFilename = (String) parts.getOrDefault("cover_filename", "cover.png");
            coverPath = FileStorageManager.saveCover(coverData, coverFilename);
        }

        Tracks track = trackDao.create(userId, title, artist, genre, description, filePath, coverPath, durationSeconds);
        if (track == null) {
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Failed to save track"));
            return;
        }

        HttpHelper.sendJsonResponse(exchange, 201, JsonHelper.dataResponse(track.toJson()));
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        int userId = HttpHelper.getAuthUserId(exchange);
        if (userId == -1) {
            HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
            return;
        }
        int trackId = extractId(path, "/api/tracks/");
        Tracks track = trackDao.findById(trackId, userId);
        if (track == null) {
            HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Track not found"));
            return;
        }
        if (track.getUserId() != userId) {
            HttpHelper.sendJsonResponse(exchange, 403, JsonHelper.errorResponse("Not authorized"));
            return;
        }
        // Delete file
        if (track.getFilePath() != null) FileStorageManager.deleteFile(track.getFilePath());
        if (track.getCoverPath() != null) FileStorageManager.deleteFile(track.getCoverPath());

        trackDao.delete(trackId);
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Track deleted"));
    }

    private void handlePlay(HttpExchange exchange, String path) throws IOException {
        String idStr = path.replace("/api/tracks/", "").replace("/play", "");
        int trackId = Integer.parseInt(idStr);
        trackDao.incrementPlayCount(trackId);
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Play count incremented"));
    }

    private int extractId(String path, String prefix) {
        String idStr = path.substring(prefix.length());
        int slash = idStr.indexOf('/');
        if (slash != -1) idStr = idStr.substring(0, slash);
        return Integer.parseInt(idStr);
    }

    private void handleTrending(HttpExchange exchange) throws IOException {
        int userId = HttpHelper.getAuthUserId(exchange);
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String period = params.getOrDefault("period", "all");
        int limit = 20;
        try { limit = Integer.parseInt(params.getOrDefault("limit", "20")); } catch (Exception e) {}
        List<Tracks> tracks = trackDao.findTrending(userId, period, limit);
        List<Map<String, Object>> jsonList = tracks.stream().map(Tracks::toJson).collect(Collectors.toList());
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.listResponse(jsonList));
    }

    private int calculateWavDuration(byte[] data) {
        try {
            if (data.length < 44) return 0;
            // WAV header: bytes 24-27 = sample rate, bytes 34-35 = bits per sample
            int sampleRate = (data[24] & 0xFF) | ((data[25] & 0xFF) << 8) | ((data[26] & 0xFF) << 16) | ((data[27] & 0xFF) << 24);
            int byteRate = (data[28] & 0xFF) | ((data[29] & 0xFF) << 8) | ((data[30] & 0xFF) << 16) | ((data[31] & 0xFF) << 24);
            if (byteRate <= 0) return 0;
            int dataSize = data.length - 44;
            return dataSize / byteRate;
        } catch (Exception e) {
            return 0;
        }
    }
}
