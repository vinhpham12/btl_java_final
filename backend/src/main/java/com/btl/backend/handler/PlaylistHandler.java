/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.handler;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.DAO.PlaylistsDAO;
import com.btl.backend.model.PlayLists;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PlaylistHandler implements HttpHandler {

    private final PlaylistsDAO playlistDao = new PlaylistsDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) { HttpHelper.handleCors(exchange); return; }

        int userId = HttpHelper.getAuthUserId(exchange);
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method) && path.equals("/api/playlists")) {
                if (userId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                List<PlayLists> playlists = playlistDao.findByUserId(userId);
                List<Map<String, Object>> jsonList = playlists.stream().map(PlayLists::toJson).collect(Collectors.toList());
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.listResponse(jsonList));

            } else if ("POST".equals(method) && path.equals("/api/playlists")) {
                if (userId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, Object> json = JsonHelper.parseJson(body);
                String name = JsonHelper.getString(json, "name");
                String desc = JsonHelper.getString(json, "description", "");
                boolean isPublic = JsonHelper.getBoolean(json, "isPublic");
                if (name == null || name.isEmpty()) { HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Name is required")); return; }
                PlayLists playlist = playlistDao.create(userId, name, desc, isPublic);
                HttpHelper.sendJsonResponse(exchange, 201, JsonHelper.dataResponse(playlist.toJson()));

            } else if ("GET".equals(method) && path.matches("/api/playlists/\\d+")) {
                int plId = Integer.parseInt(path.replace("/api/playlists/", ""));
                PlayLists playlist = playlistDao.findById(plId, userId);
                if (playlist == null) { HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Playlist not found")); return; }
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(playlist.toJson()));

            } else if ("PUT".equals(method) && path.matches("/api/playlists/\\d+")) {
                if (userId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                int plId = Integer.parseInt(path.replace("/api/playlists/", ""));
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, Object> json = JsonHelper.parseJson(body);
                playlistDao.update(plId, userId, JsonHelper.getString(json, "name"), JsonHelper.getString(json, "description"), JsonHelper.getBoolean(json, "isPublic"));
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Playlist updated"));

            } else if ("DELETE".equals(method) && path.matches("/api/playlists/\\d+")) {
                if (userId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                int plId = Integer.parseInt(path.replace("/api/playlists/", ""));
                playlistDao.delete(plId, userId);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Playlist deleted"));

            } else if ("POST".equals(method) && path.matches("/api/playlists/\\d+/tracks")) {
                if (userId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                String idStr = path.replace("/api/playlists/", "").replace("/tracks", "");
                int plId = Integer.parseInt(idStr);
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, Object> json = JsonHelper.parseJson(body);
                int trackId = JsonHelper.getInt(json, "trackId");
                playlistDao.addTrack(plId, trackId, userId);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Track added to playlist"));

            } else if ("DELETE".equals(method) && path.matches("/api/playlists/\\d+/tracks/\\d+")) {
                if (userId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                String[] parts = path.split("/");
                int plId = Integer.parseInt(parts[3]);
                int trackId = Integer.parseInt(parts[5]);
                playlistDao.removeTrack(plId, trackId, userId);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Track removed from playlist"));

            } else {
                HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Not found"));
            }
        } catch (Exception e) {
            System.err.println("[PlaylistHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}

