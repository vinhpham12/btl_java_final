/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.DAO;
import com.btl.backend.model.PlayLists;
import com.btl.backend.model.Tracks;
import com.btl.backend.util.DBConnection;
import java.sql.*;
import java.util.*;
/**
 *
 * @author ADMIN
 */
public class PlaylistsDAO {
    private PlayLists mapRow(ResultSet rs) throws SQLException {
        PlayLists p = new PlayLists();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPublic(rs.getBoolean("is_public"));
        Timestamp ts = rs.getTimestamp("created_at");
        p.setCreatedAt(ts != null ? ts.toString() : null);
        p.setOwnerName(rs.getString("owner_name"));
        return p;
    }
    public PlayLists create(int userId, String name, String description, boolean isPublic) {
        String sql = "INSERT INTO playlists (user_id, name, description, is_public) VALUES (?,?,?,?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setBoolean(4, isPublic);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return findById(rs.getInt(1), userId);
            }
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] create error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }

    public PlayLists findById(int id, int currentUserId) {
        String sql = "SELECT p.*, u.display_name AS owner_name FROM playlists p JOIN users u ON p.user_id = u.id WHERE p.id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayLists p = mapRow(rs);
                    p.setTracks(getPlaylistTracks(id, currentUserId));
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] findById error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return null;
    }

    public List<PlayLists> findByUserId(int userId) {
        String sql = "SELECT p.*, u.display_name AS owner_name, "
                   + "(SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlist_id = p.id) AS track_count "
                   + "FROM playlists p JOIN users u ON p.user_id = u.id "
                   + "WHERE p.user_id = ? ORDER BY p.created_at DESC";
        List<PlayLists> playlists = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayLists p = mapRow(rs);
                    // Dùng trackCount trực tiếp từ SQL COUNT, không tạo dummy objects
                    int count = rs.getInt("track_count");
                    List<Tracks> countList = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) countList.add(new Tracks());
                    p.setTracks(countList);
                    playlists.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] findByUserId error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return playlists;
    }

    public boolean update(int id, int userId, String name, String description, boolean isPublic) {
        String sql = "UPDATE playlists SET name=?, description=?, is_public=? WHERE id=? AND user_id=?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setBoolean(3, isPublic);
            ps.setInt(4, id);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] update error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM playlists WHERE id = ? AND user_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] delete error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    public boolean addTrack(int playlistId, int trackId, int userId) {
        // Verify ownership
        String check = "SELECT 1 FROM playlists WHERE id = ? AND user_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    DBConnection.releaseConnection(conn);
                    return false;
                }
            }
        } catch (SQLException e) {
            DBConnection.releaseConnection(conn);
            return false;
        }

        String sql = "INSERT IGNORE INTO playlist_tracks (playlist_id, track_id, position) VALUES (?, ?, (SELECT COALESCE(MAX(pt.position),0)+1 FROM playlist_tracks pt WHERE pt.playlist_id = ?))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, trackId);
            ps.setInt(3, playlistId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] addTrack error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    public boolean removeTrack(int playlistId, int trackId, int userId) {
        String sql = "DELETE pt FROM playlist_tracks pt JOIN playlists p ON pt.playlist_id = p.id WHERE pt.playlist_id = ? AND pt.track_id = ? AND p.user_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, trackId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] removeTrack error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return false;
    }

    private List<Tracks> getPlaylistTracks(int playlistId, int currentUserId) {
        String sql = """
            SELECT t.*, u.display_name AS uploader_name,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id) AS like_count,
                (SELECT COUNT(*) FROM likes WHERE track_id = t.id AND user_id = ?) AS liked
            FROM playlist_tracks pt
            JOIN tracks t ON pt.track_id = t.id
            JOIN users u ON t.user_id = u.id
            WHERE pt.playlist_id = ?
            ORDER BY pt.position
        """;
        List<Tracks> tracks = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tracks t = new Tracks();
                    t.setId(rs.getInt("id"));
                    t.setUserId(rs.getInt("user_id"));
                    t.setTitle(rs.getString("title"));
                    t.setArtist(rs.getString("artist"));
                    t.setGenre(rs.getString("genre"));
                    t.setDurationSeconds(rs.getInt("duration_seconds"));
                    t.setPlayCount(rs.getInt("play_count"));
                    t.setUploaderName(rs.getString("uploader_name"));
                    t.setLikeCount(rs.getInt("like_count"));
                    t.setLikedByCurrentUser(rs.getInt("liked") > 0);
                    Timestamp ts = rs.getTimestamp("created_at");
                    t.setCreatedAt(ts != null ? ts.toString() : null);
                    tracks.add(t);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlaylistDao] getPlaylistTracks error: " + e.getMessage());
        } finally {
            DBConnection.releaseConnection(conn);
        }
        return tracks;
    }

    
}
