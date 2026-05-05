/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.DAO;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.model.Comments;
import com.btl.backend.util.DBConnection;
import java.sql.*;
import java.util.*;
public class CommentsDAO {
    private Comments mapRow(ResultSet rs) throws SQLException {
        Comments c = new Comments();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setTrackId(rs.getInt("track_id"));
        c.setContent(rs.getString("content"));
        c.setTimestampSeconds(rs.getInt("timestamp_seconds"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts != null ? ts.toString() : null);
        c.setUsername(rs.getString("username"));
        c.setDisplayName(rs.getString("display_name"));
        return c;
    }
    //tao comments 
    public Comments create(int userId, int trackId, String content, int timestampSeconds) {
        String sql = "INSERT INTO comments (user_id, track_id, content, timestamp_seconds) VALUES (?,?,?,?)";//khi comment luu toan bo du lieu vao bang comments
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            ps.setString(3, content);
            ps.setInt(4, timestampSeconds);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return findById(rs.getInt(1));//dua id comment vua tao vao de lay thong tin tra ve frontend hien thi 
        } catch (SQLException e) {
            System.err.println("[CommentDao] create error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }
    //lay thong tin nguoi dung binh luan 
    public Comments findById(int id) {
        String sql = "SELECT c.*, u.username, u.display_name FROM comments c JOIN users u ON c.user_id = u.id WHERE c.id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[CommentDao] findById error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }
    //tai dang sach binh luan len giao dien cua track
    public List<Comments> findByTrackId(int trackId) {
        //dua ra toan bo thong tin nguoi binh luan va binh luan de hien thi va sap xep binh luan theo moc thoi gian bai hat
        String sql = "SELECT c.*, u.username, u.display_name FROM comments c JOIN users u ON c.user_id = u.id WHERE c.track_id = ? ORDER BY c.timestamp_seconds ASC";
        List<Comments> comments = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trackId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) comments.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CommentDao] findByTrackId error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return comments;
    }
    //xoa binh luan
    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM comments WHERE id = ? AND user_id = ?";//cho phep xoa binh luan neu trung ten nguoi dung 
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;//tra ve true khi da xoa 
        } catch (SQLException e) {
            System.err.println("[CommentDao] delete error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    
}
