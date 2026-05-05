/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.handler;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.DAO.*;
import com.btl.backend.model.*;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SearchHandler implements HttpHandler {

    private final TracksDAO trackDao = new TracksDAO();
    private final UsersDAO userDao = new UsersDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) { HttpHelper.handleCors(exchange); return; }
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpHelper.sendJsonResponse(exchange, 405, JsonHelper.errorResponse("Method not allowed"));
            return;
        }

        try {
            int userId = HttpHelper.getAuthUserId(exchange);
            Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
            String query = params.getOrDefault("q", "");
            String type = params.getOrDefault("type", "all");

            Map<String, Object> result = new LinkedHashMap<>();

            if ("track".equals(type) || "all".equals(type)) {
                List<Tracks> tracks = trackDao.search(query, userId);
                result.put("tracks", tracks.stream().map(Tracks::toJson).collect(Collectors.toList()));
            }
            if ("user".equals(type) || "all".equals(type)) {
                List<Users> users = userDao.search(query);
                result.put("users", users.stream().map(Users::toJson).collect(Collectors.toList()));
            }

            HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(result));
        } catch (Exception e) {
            System.err.println("[SearchHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}
