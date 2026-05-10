package com.btl.backend.DAO;

import com.btl.backend.util.DBConnection;
import java.sql.*;
import java.util.*;

/**
 * DAO cho repost (đăng lại bài hát lên profile).
 */
public class RepostDAO {

    public boolean repost(int userId, int trackId) {
        String sql = "INSERT IGNORE INTO reposts (user_id, track_id) VALUES (?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepostDAO] repost error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    public boolean unrepost(int userId, int trackId) {
        String sql = "DELETE FROM reposts WHERE user_id = ? AND track_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepostDAO] unrepost error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    public boolean isReposted(int userId, int trackId) {
        String sql = "SELECT 1 FROM reposts WHERE user_id = ? AND track_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[RepostDAO] isReposted error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    /**
     * Lấy danh sách bài đã repost của user (cho trang profile).
     */
    public List<Map<String, Object>> getRepostsByUser(int userId, int currentUserId) {
        String sql = """
            SELECT t.*, u.display_name AS uploader_name, r.created_at AS reposted_at,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id) AS like_count,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id AND user_id = ?) AS liked
            FROM reposts r
            JOIN tracks t ON r.track_id = t.id
            JOIN users u ON t.user_id = u.id
            WHERE r.user_id = ?
            ORDER BY r.created_at DESC
        """;
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("userId", rs.getInt("user_id"));
                    item.put("title", rs.getString("title"));
                    item.put("artist", rs.getString("artist"));
                    item.put("genre", rs.getString("genre"));
                    item.put("durationSeconds", rs.getInt("duration_seconds"));
                    item.put("playCount", rs.getInt("play_count"));
                    item.put("uploaderName", rs.getString("uploader_name"));
                    item.put("likeCount", rs.getInt("like_count"));
                    item.put("liked", rs.getInt("liked") > 0);
                    item.put("repostedAt", rs.getTimestamp("reposted_at").toString());
                    result.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepostDAO] getRepostsByUser error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return result;
    }
}
