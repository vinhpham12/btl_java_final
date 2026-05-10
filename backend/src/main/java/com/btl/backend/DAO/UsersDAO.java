/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.DAO;//truy van du lieu co so va them du lieu co so giao tiep truc tiep voi mysql

import com.btl.backend.model.Users;
import com.btl.backend.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ADMIN
 */
public class UsersDAO {
    private Users mapRow(ResultSet rs) throws SQLException{//ham tao doi tuong user lay toan bo thuoc tinh cua user 
        Users user= new Users();
        user.setId(rs.getInt("id"));//user set thanh phan tu nhung gi rs lay ra tu cot id
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));
        user.setBio(rs.getString("bio"));
        user.setAvatarPath(rs.getString("avatar_path"));
        Timestamp ts = rs.getTimestamp("created_at");
        user.setCreatedAt(ts != null ? ts.toString() : null);//neu co du lieu thi ep thanh string khong co tra ve null
        return user;
    }
    public Users create(String username,String passwordHash,String displayName) throws SQLException{
        String sql="INSERT INTO users(username,password_hash,display_name) VALUES (?,?,?)";
        Connection conn=DBConnection.getConnection();
        try(PreparedStatement ps= conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){//khi tao user moi se tu dong sinh 1 id
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3,displayName);
            ps.executeUpdate();
            try(ResultSet rs= ps.getGeneratedKeys()) {
                if(rs.next()){
                    return findById(rs.getInt(1));// lay id vua duoc tao ra de dua vao findbyid truy van csdl lay 1 user hoan chinh
                }
            }
        }finally{
            DBConnection.releaseConnection(conn); //sau khi dung xong tra ve cho pool
        }
        return null;
    }
    
    //tim bang id
    public Users findById(int id) {
        String sql = """
            SELECT u.*, 
                (SELECT COUNT(*) FROM tracks WHERE user_id = u.id) AS track_count,
                (SELECT COUNT(*) FROM follows WHERE following_id = u.id) AS follower_count,
                (SELECT COUNT(*) FROM follows WHERE follower_id = u.id) AS following_count
            FROM users u WHERE u.id = ?
        """;
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Users user = mapRow(rs);
                    user.setTrackCount(rs.getInt("track_count"));
                    user.setFollowerCount(rs.getInt("follower_count"));
                    user.setFollowingCount(rs.getInt("following_count"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] findById error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }
    
    //tim bang username
    public Users findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] findByUsername error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }
    
    //sua
    public boolean updateProfile(int id, String displayName, String bio) {
        String sql = "UPDATE users SET display_name = ?, bio = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, bio);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] updateProfile error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
    
    //sua
    public boolean updateAvatar(int id, String avatarPath) {
        String sql = "UPDATE users SET avatar_path = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, avatarPath);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] updateAvatar error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
    
    //tim kiem bang ca username va display name
    public List<Users> search(String query) {
        String sql = "SELECT * FROM users WHERE username LIKE ? OR display_name LIKE ? LIMIT 20";
        List<Users> users = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] search error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return users;
    }

    // follow
    public boolean follow(int followerId, int followingId) {
        String sql = "INSERT IGNORE INTO follows (follower_id, following_id) VALUES (?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] follow error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
    
    public boolean unfollow(int followerId, int followingId) {
        String sql = "DELETE FROM follows WHERE follower_id = ? AND following_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] unfollow error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    public boolean isFollowing(int followerId, int followingId) {
        String sql = "SELECT 1 FROM follows WHERE follower_id = ? AND following_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] isFollowing error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }
}
