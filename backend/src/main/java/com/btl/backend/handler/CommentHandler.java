/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.handler;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.DAO.CommentsDAO;
import com.btl.backend.DAO.NotificationDAO;
import com.btl.backend.DAO.TracksDAO;
import com.btl.backend.model.Comments;
import com.btl.backend.model.Tracks;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CommentHandler implements HttpHandler {

    private final CommentsDAO commentDao = new CommentsDAO();
    private final NotificationDAO notifDao = new NotificationDAO();
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
            if (path.startsWith("/api/tracks/") && path.endsWith("/comments")) {
                String idStr = path.replace("/api/tracks/", "").replace("/comments", "");
                int trackId = Integer.parseInt(idStr);

                if ("GET".equals(method)) {
                    List<Comments> comments = commentDao.findByTrackId(trackId);
                    List<Map<String, Object>> jsonList = comments.stream().map(Comments::toJson).collect(Collectors.toList());
                    HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.listResponse(jsonList));
                } else if ("POST".equals(method)) {
                    int userId = HttpHelper.getAuthUserId(exchange);
                    if (userId == -1) {
                        HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
                        return;
                    }
                    String body = HttpHelper.readRequestBody(exchange);
                    Map<String, Object> json = JsonHelper.parseJson(body);
                    String content = JsonHelper.getString(json, "content");
                    int timestamp = JsonHelper.getInt(json, "timestampSeconds");

                    if (content == null || content.isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Content is required"));
                        return;
                    }

                    Comments comment = commentDao.create(userId, trackId, content, timestamp);
                    if (comment != null) {
                        // Tạo notification cho chủ track
                        Tracks track = trackDao.findById(trackId, userId);
                        if (track != null && track.getUserId() != userId) {
                            notifDao.create(track.getUserId(), "comment",
                                "đã bình luận vào bài hát \"" + track.getTitle() + "\"",
                                userId, trackId);
                        }
                        HttpHelper.sendJsonResponse(exchange, 201, JsonHelper.dataResponse(comment.toJson()));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Failed to create comment"));
                    }
                }
            } else if (path.startsWith("/api/comments/") && "DELETE".equals(method)) {
                int userId = HttpHelper.getAuthUserId(exchange);
                if (userId == -1) {
                    HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
                    return;
                }
                int commentId = Integer.parseInt(path.replace("/api/comments/", ""));
                if (commentDao.delete(commentId, userId)) {
                    HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Comment deleted"));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Comment not found or not authorized"));
                }
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Not found"));
            }
        } catch (Exception e) {
            System.err.println("[CommentHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}

