/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.DAO;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.model.Tracks;
import com.btl.backend.util.DBConnection;
import java.sql.*;
import java.util.*;
public class TracksDAO {
    //tao maprow nhu user
    private Tracks mapRow(ResultSet rs) throws SQLException {
        Tracks t = new Tracks();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setTitle(rs.getString("title"));
        t.setArtist(rs.getString("artist"));
        t.setGenre(rs.getString("genre"));
        t.setDescription(rs.getString("description"));
        t.setFilePath(rs.getString("file_path"));
        t.setCoverPath(rs.getString("cover_path"));
        t.setDurationSeconds(rs.getInt("duration_seconds"));
        t.setPlayCount(rs.getInt("play_count"));
        Timestamp ts = rs.getTimestamp("created_at");
        t.setCreatedAt(ts != null ? ts.toString() : null);
        t.setUploaderName(rs.getString("uploader_name"));
        t.setLikeCount(rs.getInt("like_count"));
        t.setLikedByCurrentUser(rs.getInt("liked") > 0);
        return t;
    }
    
    
    //them bai hat moi
    public Tracks create(int userId, String title, String artist, String genre, String description, String filePath, String coverPath, int durationSeconds) {
        String sql = "INSERT INTO tracks (user_id, title, artist, genre, description, file_path, cover_path, duration_seconds) VALUES (?,?,?,?,?,?,?,?)";
        Connection conn = DBConnection.getConnection(); // connection
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, artist);
            ps.setString(4, genre);
            ps.setString(5, description);
            ps.setString(6, filePath);
            ps.setString(7, coverPath);
            ps.setInt(8, durationSeconds);
            ps.executeUpdate(); //INSERT

            // lay id tu dong tang vua duoc tao
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return findById(rs.getInt(1), -1);
        } catch (SQLException e) {
            System.err.println("[TrackDao] create error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }

    //tim bai hat theo id va cho biet nguoi dung dang dang nhap da like chua
    public Tracks findById(int id, int currentUserId) {
        //cau lenh sql
        //JOIN users de lay display_name cua nguoi upload
        //truy van con dem tong so like cua bai hat
        //truy van con curentuser da like chua
        String sql = """
            SELECT t.*, u.display_name AS uploader_name,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id) AS like_count,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id AND user_id = ?) AS liked
            FROM tracks t JOIN users u ON t.user_id = u.id
            WHERE t.id = ?
        """;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[TrackDao] findById error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }

    //lay danh sach de hien len man hinh user theo danh sach phan trang da sap xep
    public List<Tracks> findAll(int currentUserId, int offset, int limit, String sortBy) {
        // Quyết định field để ORDER BY
        String orderClause = switch (sortBy != null ? sortBy : "newest") {
            case "popular" -> "t.play_count DESC"; // nghe nhgieu nhat
            case "likes" -> "like_count DESC";     // like nhieu nhat
            default -> "t.created_at DESC";        // moi nhat
        };
        String sql = """
            SELECT t.*, u.display_name AS uploader_name,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id) AS like_count,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id AND user_id = ?) AS liked
            FROM tracks t JOIN users u ON t.user_id = u.id
            ORDER BY %s LIMIT ? OFFSET ?
        """.formatted(orderClause);
        return queryTracks(sql, currentUserId, limit, offset);
    }
    
    //lay cac cau lenh sql trong findall de tim dong thanh list 
    private List<Tracks> queryTracks(String sql, int currentUserId, int limit, int offset) {
        Connection conn = DBConnection.getConnection();
        List<Tracks> tracks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tracks.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[TrackDao] queryTracks error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return tracks;
    }

    //tim bai hat cua 1 user dang tai trong profile page
    public List<Tracks> findByUserId(int userId, int currentUserId) {
        String sql = """
            SELECT t.*, u.display_name AS uploader_name,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id) AS like_count,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id AND user_id = ?) AS liked
            FROM tracks t JOIN users u ON t.user_id = u.id
            WHERE t.user_id = ? ORDER BY t.created_at DESC
        """;
        Connection conn = DBConnection.getConnection();
        List<Tracks> tracks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tracks.add(mapRow(rs)); //chuyen doi tung dong ResultSet thanh Track
        } catch (SQLException e) {
            System.err.println("[TrackDao] findByUserId error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return tracks;
    }

    //tim kiem bai hat theo tu khoa
    public List<Tracks> search(String query, int currentUserId) {
        String sql = """
            SELECT t.*, u.display_name AS uploader_name,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id) AS like_count,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id AND user_id = ?) AS liked
            FROM tracks t JOIN users u ON t.user_id = u.id
            WHERE t.title LIKE ? OR t.artist LIKE ? OR t.genre LIKE ?
            ORDER BY t.play_count DESC LIMIT 50
        """;
        Connection conn = DBConnection.getConnection();
        List<Tracks> tracks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%"; //them ki tu tieu bieu LIKE
            ps.setInt(1, currentUserId);
            ps.setString(2, pattern); //tim trong title
            ps.setString(3, pattern); //tim trong artist
            ps.setString(4, pattern); //tim trong genre
            ResultSet rs = ps.executeQuery();
            while (rs.next()) tracks.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[TrackDao] search error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return tracks;
    }
    
    //xoa bai hat
    public boolean delete(int id) {
        String sql = "DELETE FROM tracks WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TrackDao] delete error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    //dem so luot nghe 
    public void incrementPlayCount(int id) {
        String sql = "UPDATE tracks SET play_count = play_count + 1 WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[TrackDao] incrementPlayCount error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
    }
    
    //lay duong dan file goc
    public String getFilePath(int id) {
        String sql = "SELECT file_path FROM tracks WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("file_path");
        } catch (SQLException e) {
            System.err.println("[TrackDao] getFilePath error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }

}
