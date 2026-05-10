package com.btl.backend.handler;

import com.btl.backend.DAO.NotificationDAO;
import com.btl.backend.DAO.RepostDAO;
import com.btl.backend.DAO.TracksDAO;
import com.btl.backend.model.Tracks;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;

/**
 * Handler cho repost.
 * POST /api/tracks/{id}/repost — repost bài hát
 * DELETE /api/tracks/{id}/repost — unrepost
 * GET /api/users/{id}/reposts — lấy danh sách repost của user
 */
public class RepostHandler implements HttpHandler {

    private final RepostDAO repostDao = new RepostDAO();
    private final TracksDAO trackDao = new TracksDAO();
    private final NotificationDAO notifDao = new NotificationDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            // GET /api/users/{id}/reposts
            if ("GET".equals(method) && path.matches("/api/users/\\d+/reposts")) {
                int userId = HttpHelper.getAuthUserId(exchange);
                String idStr = path.replaceAll("/api/users/(\\d+)/reposts", "$1");
                int profileUserId = Integer.parseInt(idStr);
                List<Map<String, Object>> reposts = repostDao.getRepostsByUser(profileUserId, userId > 0 ? userId : -1);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "success");
                response.put("data", reposts);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(response));
                return;
            }

            // POST/DELETE /api/tracks/{id}/repost
            int userId = HttpHelper.getAuthUserId(exchange);
            if (userId == -1) {
                HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
                return;
            }

            String idStr = path.replace("/api/tracks/", "").replace("/repost", "");
            int trackId = Integer.parseInt(idStr);

            if ("POST".equals(method)) {
                boolean success = repostDao.repost(userId, trackId);
                // Tạo notification cho chủ track
                if (success) {
                    Tracks track = trackDao.findById(trackId, userId);
                    if (track != null && track.getUserId() != userId) {
                        notifDao.create(track.getUserId(), "repost",
                            "đã repost bài hát \"" + track.getTitle() + "\" của bạn",
                            userId, trackId);
                    }
                }
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("reposted", true);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(data));

            } else if ("DELETE".equals(method)) {
                repostDao.unrepost(userId, trackId);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("reposted", false);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(data));

            } else {
                HttpHelper.sendJsonResponse(exchange, 405, JsonHelper.errorResponse("Method not allowed"));
            }
        } catch (Exception e) {
            System.err.println("[RepostHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}
