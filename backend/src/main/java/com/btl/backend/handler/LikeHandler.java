/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.handler;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.DAO.LikeDAO;
import com.btl.backend.DAO.NotificationDAO;
import com.btl.backend.DAO.TracksDAO;
import com.btl.backend.model.Tracks;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;

public class LikeHandler implements HttpHandler {

    private final LikeDAO likeDao = new LikeDAO();
    private final NotificationDAO notifDao = new NotificationDAO();
    private final TracksDAO trackDao = new TracksDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        int userId = HttpHelper.getAuthUserId(exchange);
        if (userId == -1) {
            HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            // /api/tracks/{id}/like
            String idStr = path.replace("/api/tracks/", "").replace("/like", "");
            int trackId = Integer.parseInt(idStr);

            if ("POST".equals(method)) {
                likeDao.like(userId, trackId);
                // Tạo notification cho chủ track
                Tracks track = trackDao.findById(trackId, userId);
                if (track != null && track.getUserId() != userId) {
                    notifDao.create(track.getUserId(), "like",
                        "đã thích bài hát \"" + track.getTitle() + "\" của bạn",
                        userId, trackId);
                }
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("liked", true);
                data.put("likeCount", likeDao.getLikeCount(trackId));
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(data));
            } else if ("DELETE".equals(method)) {
                likeDao.unlike(userId, trackId);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("liked", false);
                data.put("likeCount", likeDao.getLikeCount(trackId));
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(data));
            } else {
                HttpHelper.sendJsonResponse(exchange, 405, JsonHelper.errorResponse("Method not allowed"));
            }
        } catch (Exception e) {
            System.err.println("[LikeHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}

