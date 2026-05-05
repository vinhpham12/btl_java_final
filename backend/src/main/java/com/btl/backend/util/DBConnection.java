/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.util;

/**
 *
 * @author ADMIN
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
public class DBConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/app_phat_nhac";
    private static final String USER = "root";
    private static final String PASS = ""; 
    private static final int pool_Size=10;//tao 1 be chua co san 10 connection
    private static BlockingQueue<Connection> connectionPool;//goi ra 1 hang doi Thread safe(khi co 12 nguoi muon truy cap thi se 2 nguoi truy cap sau nguoi thu 10 vao hang doi lay ket noi
    private static boolean initialized= false;//khoi tao co danh dau da khoi tao chua
    
    public static synchronized void initialize(){//ham khoi tao conection pool
        if(initialized) return ;//da khoi tao roi thi bo qua
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");//goi thu vien mysql connector
            connectionPool= new ArrayBlockingQueue<>(pool_Size);
            for(int i=0;i<pool_Size;i++){    
                connectionPool.add(createConnection());
            }
            initialized =true;
            System.out.println("[DB] Connection pool initialized with "+pool_Size+" connections");
            initializedTables();
        }catch(Exception e){
            System.err.println("[DB] Failed to initialize: "+e.getMessage());//bao loi 
            throw new RuntimeException("Database initialization failed",e);//tao ra 1 loi he thong runtime de dong server
        }
    }
    
    // tao 1 connect moi den mysql
    public static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
    
    //muon 1 connection tu pool
    public static Connection getConnection(){
        try{
            Connection conn =connectionPool.take();//lay tu pool
            if(conn.isClosed()){
                conn= createConnection();
            }
            return conn;
        }catch(Exception e){
            throw new RuntimeException("Failed to get connection",e);//neu lay ket noi that bai thi tao loi runtime dong server
        }
    }
    
    public static void releaseConnection(Connection conn){//tra connection lai pool khi su dung xong
        if(conn!=null){
            try{
                if(!conn.isClosed()){
                    connectionPool.offer(conn);//tra lai pool neu chua bi dong do timeout
                }
                else{
                    connectionPool.offer(createConnection());//neu khong thi tra bang connection moi 
                }
            }
            catch(SQLException e){
                System.out.println("[DB] Error releasing connection: "+e.getMessage());
            }
        }
    }
    private static void initializedTables() {//tu dong tao cac bang neu thieu trong database
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Users 
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    display_name VARCHAR(100) NOT NULL,
                    bio TEXT,
                    avatar_path VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Tracks 
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tracks (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    artist VARCHAR(200),
                    genre VARCHAR(50),
                    description TEXT,
                    file_path VARCHAR(500) NOT NULL,
                    cover_path VARCHAR(500),
                    duration_seconds INT DEFAULT 0,
                    play_count INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // Playlists
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS playlists (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    name VARCHAR(200) NOT NULL,
                    description TEXT,
                    is_public BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // Playlist tracks 
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS playlist_tracks (
                    playlist_id INT NOT NULL,
                    track_id INT NOT NULL,
                    position INT NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (playlist_id, track_id),
                    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                )
            """);

            // Likes 
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS likes (
                    user_id INT NOT NULL,
                    track_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, track_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                )
            """);

            // Comments 
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS comments (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    track_id INT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp_seconds INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                )
            """);
            // Follows
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS follows (
                    follower_id INT NOT NULL,
                    following_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (follower_id, following_id),
                    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            System.out.println("[DB] Tao bang thanh cong");
        } catch (SQLException e) {
            System.err.println("[DB] Loi tao bang: " + e.getMessage());
        } finally {
            releaseConnection(conn);
        }
    }
    
    public static void shutdown(){//dong tat ca connection khi server tat
        if(connectionPool!=null){
            for(Connection conn: connectionPool){
                try{
                    conn.close();
                }
                catch(SQLException e){
                };
            }
        }
        System.out.println("[DB] Da dong tat ca cong ket noi");
    }
    
//    public static void main(String[] args) {
//        try {
//            Connection conn = getConnection();
//            System.out.println("Kết nối MySQL thành công!");
//            conn.close();
//        } catch (SQLException e) {
//            System.out.println("Lỗi kết nối: " + e.getMessage());
//        }
//    }

    

   
}
