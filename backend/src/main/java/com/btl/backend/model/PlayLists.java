/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ADMIN
 */
public class PlayLists {
    private int id;//id playlist
    private int userId;//id nguoi tao playlist
    private String name;//ten playlist
    private String description;//mo ta
    private boolean isPublic;//false la rieng tu ,true la public
    private String createdAt;//thoi gian tao
    private String ownerName;//nguoi so huu
    private List<Tracks> tracks = new ArrayList<>();//1 playlist co the co nhieu bai hat(track)

    public PlayLists() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public List<Tracks> getTracks() { return tracks; }
    public void setTracks(List<Tracks> tracks) { this.tracks = tracks; }

    public Map<String, Object> toJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("name", name);
        map.put("description", description);
        map.put("isPublic", isPublic);
        map.put("createdAt", createdAt);
        map.put("ownerName", ownerName);
        map.put("trackCount", tracks.size());
        List<Object> trackList = new ArrayList<>();
        for (Tracks t : tracks) trackList.add(t.toJson());
        map.put("tracks", trackList);
        return map;
    }
}
