package com.btl.backend.DAO;

import com.btl.backend.util.DBConnection;
import java.sql.*;
import java.util.*;

/**
 * DAO cho lịch sử nghe nhạc.
 */
public class HistoryDAO {

    /**
     * Ghi lại 1 lượt nghe.
     */
    public void addHistory(int userId, int trackId) {
        String sql = "INSERT INTO listening_history (user_id, track_id) VALUES (?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[HistoryDAO] addHistory error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
    }

    /**
     * Lấy danh sách bài đã nghe gần đây (JOIN tracks + users để lấy đủ thông tin).
     */
    public List<Map<String, Object>> getHistory(int userId, int limit) {
        String sql = """
            SELECT h.id AS history_id, h.listened_at, 
                   t.*, u.display_name AS uploader_name,
                   (SELECT COUNT(*) FROM likes WHERE track_id = t.id) AS like_count,
                   (SELECT COUNT(*) FROM likes WHERE track_id = t.id AND user_id = ?) AS liked
            FROM listening_history h
            JOIN tracks t ON h.track_id = t.id
            JOIN users u ON t.user_id = u.id
            WHERE h.user_id = ?
            ORDER BY h.listened_at DESC
            LIMIT ?
        """;
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("historyId", rs.getInt("history_id"));
                    item.put("listenedAt", rs.getTimestamp("listened_at").toString());
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
                    result.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("[HistoryDAO] getHistory error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return result;
    }

    /**
     * Xóa toàn bộ lịch sử nghe của user.
     */
    public boolean clearHistory(int userId) {
        String sql = "DELETE FROM listening_history WHERE user_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[HistoryDAO] clearHistory error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
}
