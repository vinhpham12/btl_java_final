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
public class Tracks {
    private int id;//id bai hat
    private int userId;//uploader
    private String title;//ten bai hat
    private String artist;//nguoi hat
    private String genre;//the loai
    private String description;//mo ta
    private String filePath;//duong dan toi file am thanh goc 
    private String coverPath;//duong dan toi anh bia bai hat
    private int durationSeconds;//thoi luong tinh bang giay
    private int playCount;//so lan nghe 
    private String createdAt;//thoi gian tao
    private String uploaderName;//ten nguoi uploader
    private int likeCount;//so luong like
    private boolean likedByCurrentUser;//trang thai da thich (khi 1 nguoi nghe lai thi co hien la da like khong)

    public Tracks() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean liked) { this.likedByCurrentUser = liked; }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("title", title);
        map.put("artist", artist);
        map.put("genre", genre);
        map.put("description", description);
        map.put("coverPath", coverPath);
        map.put("durationSeconds", durationSeconds);
        map.put("playCount", playCount);
        map.put("createdAt", createdAt);
        map.put("uploaderName", uploaderName);
        map.put("likeCount", likeCount);
        map.put("likedByCurrentUser", likedByCurrentUser);
        return map;
    }
}
