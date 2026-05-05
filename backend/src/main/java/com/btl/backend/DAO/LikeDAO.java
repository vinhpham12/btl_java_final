/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.DAO;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.util.DBConnection;
import java.sql.*;
public class LikeDAO {
    //like
    public boolean like(int userId, int trackId) {
        String sql = "INSERT IGNORE INTO likes (user_id, track_id) VALUES (?, ?)";//su dung INSERT IGNORE de tranh khi nguoi dung an nhieu lan thi gui request giong nhau lien tuc 
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            return ps.executeUpdate() > 0;// tra ve true neu dong moi duoc them vao
        } catch (SQLException e) {
            System.err.println("[LikeDao] like error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
    //unlike
    public boolean unlike(int userId, int trackId) {
        String sql = "DELETE FROM likes WHERE user_id = ? AND track_id = ?";//xoa csdl o bang like cua nguoi dung 
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            return ps.executeUpdate() > 0;//tra ve true neu xoa thanh cong
        } catch (SQLException e) {
            System.err.println("[LikeDao] unlike error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
    //dang like
    public boolean isLiked(int userId, int trackId) {
        String sql = "SELECT 1 FROM likes WHERE user_id = ? AND track_id = ?";//truy xuat trang thai nguoi dung da like chua 
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, trackId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[LikeDao] isLiked error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
    //dem so like 
    public int getLikeCount(int trackId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE track_id = ?";//dem tong hop so like cua track id do 
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trackId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[LikeDao] getLikeCount error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return 0;
    }
}
