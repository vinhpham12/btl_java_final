package com.btl.backend.DAO;

import com.btl.backend.util.DBConnection;
import java.sql.*;
import java.util.*;

/**
 * DAO cho thông báo (notifications).
 */
public class NotificationDAO {

    /**
     * Tạo thông báo mới.
     * @param type: "like", "comment", "follow", "repost"
     */
    public void create(int userId, String type, String message, int fromUserId, int trackId) {
        String sql = "INSERT INTO notifications (user_id, type, message, from_user_id, track_id) VALUES (?,?,?,?,?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, message);
            ps.setInt(4, fromUserId);
            if (trackId > 0) ps.setInt(5, trackId);
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] create error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
    }

    /**
     * Lấy danh sách thông báo của user (mới nhất trước).
     */
    public List<Map<String, Object>> getNotifications(int userId, int limit) {
        String sql = """
            SELECT n.*, u.display_name AS from_name, u.avatar_path AS from_avatar
            FROM notifications n
            LEFT JOIN users u ON n.from_user_id = u.id
            WHERE n.user_id = ?
            ORDER BY n.created_at DESC
            LIMIT ?
        """;
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("type", rs.getString("type"));
                    item.put("message", rs.getString("message"));
                    item.put("fromUserId", rs.getInt("from_user_id"));
                    item.put("fromName", rs.getString("from_name"));
                    item.put("trackId", rs.getInt("track_id"));
                    item.put("isRead", rs.getBoolean("is_read"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    item.put("createdAt", ts != null ? ts.toString() : null);
                    result.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] getNotifications error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return result;
    }

    /**
     * Đánh dấu 1 thông báo đã đọc.
     */
    public boolean markAsRead(int id, int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] markAsRead error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc.
     */
    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] markAllAsRead error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    /**
     * Đếm số thông báo chưa đọc.
     */
    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] getUnreadCount error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return 0;
    }
}
