/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author ADMIN
 */
public class Comments {
    private int id;//id comments
    private int userId;//id nguoi viet
    private int trackId;//binh luan cho bai nao
    private String content;//noi dung binh luan
    private int timestampSeconds;//thoi diem tren bai hat khi nguoi nghe binh luan
    private String createdAt;//thoi gian tao
    private String username;//ten dang nhap
    private String displayName;//ten hien thi 

    public Comments() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getTimestampSeconds() { return timestampSeconds; }
    public void setTimestampSeconds(int ts) { this.timestampSeconds = ts; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("trackId", trackId);
        map.put("content", content);
        map.put("timestampSeconds", timestampSeconds);
        map.put("createdAt", createdAt);
        map.put("username", username);
        map.put("displayName", displayName);
        return map;
    }
}
