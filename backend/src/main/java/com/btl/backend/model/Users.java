/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.model;// nhung doi tuong duoc tao ra de luu tru du lieu

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author ADMIN
 */
public class Users {
    private int id;
    private String username;//ten dang nhap
    private String passwordHash;//mk ma hoa
    private String displayName;//ten hien thi
    private String bio;//trang ca nhan
    private String avatarPath;//duong link toi anh ca nhan
    private String createdAt;//thoi gian tao
    private int trackCount;//tong so bai hat dang tai
    private int followerCount;
    private int followingCount;

    public Users() {}

    public Users(int id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public int getTrackCount() { return trackCount; }
    public void setTrackCount(int trackCount) { this.trackCount = trackCount; }
    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public Map<String, Object> toJson() {//dong goi du lien thanh 1 chuoi van ban quy dinh chung de backend co the giao tiep voi frontend
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("username", username);
        map.put("displayName", displayName);
        map.put("bio", bio);
        map.put("avatarPath", avatarPath);
        map.put("createdAt", createdAt);
        map.put("trackCount", trackCount);
        map.put("followerCount", followerCount);
        map.put("followingCount", followingCount);
        return map;
    }
}
