/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.handler;//khi frontend goi backend handler dong vai tro router/cotroller dua tren http(ngon ngu chung giao tiep)

import com.btl.backend.DAO.TracksDAO;
import com.btl.backend.DAO.UsersDAO;
import com.btl.backend.util.HttpHelper;
import com.sun.net.httpserver.HttpExchange;//http server vo cung tho chi co inputstream va outputstream nen can phai xay dung httphelper de xu li yeu cau tu frontend 
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.btl.backend.model.*;
import com.btl.backend.util.*;

/**
 *
 * @author ADMIN
 */
public class UserHandler implements HttpHandler{//nhan yeu cau tu frontend va xu ly va tra ve ket qua
    private final UsersDAO userDao = new UsersDAO();
    private final TracksDAO trackDao = new TracksDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) { HttpHelper.handleCors(exchange); return; }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        int currentUserId = HttpHelper.getAuthUserId(exchange);

        try {
            if ("GET".equals(method) && path.matches("/api/users/\\d+")) {
                int id = Integer.parseInt(path.replace("/api/users/", ""));
                Users user = userDao.findById(id);
                if (user == null) { HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("User not found")); return; }
                Map<String, Object> data = user.toJson();
                data.put("isFollowing", currentUserId > 0 && userDao.isFollowing(currentUserId, id));
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(data));

            } else if ("PUT".equals(method) && path.matches("/api/users/\\d+")) {
                if (currentUserId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                int id = Integer.parseInt(path.replace("/api/users/", ""));
                if (id != currentUserId) { HttpHelper.sendJsonResponse(exchange, 403, JsonHelper.errorResponse("Not authorized")); return; }
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, Object> json = JsonHelper.parseJson(body);
                userDao.updateProfile(id, JsonHelper.getString(json, "displayName"), JsonHelper.getString(json, "bio"));
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Profile updated"));

            } else if ("GET".equals(method) && path.matches("/api/users/\\d+/tracks")) {
                String idStr = path.replace("/api/users/", "").replace("/tracks", "");
                int id = Integer.parseInt(idStr);
                List<Tracks> tracks = trackDao.findByUserId(id, currentUserId);
                List<Map<String, Object>> jsonList = tracks.stream().map(Tracks::toJson).collect(Collectors.toList());
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.listResponse(jsonList));

            } else if ("POST".equals(method) && path.matches("/api/users/\\d+/follow")) {
                if (currentUserId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                String idStr = path.replace("/api/users/", "").replace("/follow", "");
                int id = Integer.parseInt(idStr);
                userDao.follow(currentUserId, id);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Followed"));

            } else if ("DELETE".equals(method) && path.matches("/api/users/\\d+/follow")) {
                if (currentUserId == -1) { HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated")); return; }
                String idStr = path.replace("/api/users/", "").replace("/follow", "");
                int id = Integer.parseInt(idStr);
                userDao.unfollow(currentUserId, id);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Unfollowed"));

            } else {
                HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Not found"));
            }
        } catch (Exception e) {
            System.err.println("[UserHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
    
}
