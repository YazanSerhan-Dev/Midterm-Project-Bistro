package DataBase.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.SubscriberDTO;

public class SubscriberDAO {

    public static void insertSubscriber(
            String username, String password, String name,
            String phone, String email,
            String memberCode, String barcode, Date birthDate) throws Exception {

        String sql = """
            INSERT INTO subscribers
                (username, password, name, phone, email, member_code, barcode_data, birth_date)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            ps.setString(4, phone);
            ps.setString(5, email);
            ps.setString(6, memberCode);
            ps.setString(7, barcode);
            ps.setDate(8, birthDate);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // ============================================
    // YOUR CODE (From HEAD) - Get List for Agent
    // ============================================
    public static List<SubscriberDTO> getAllSubscribers() throws Exception {
        List<SubscriberDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM subscribers"; 

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Adjust ID fetching if you have an actual ID column
                int id = 0; 
                // int id = rs.getInt("subscriber_id"); // Uncomment if you have this column

                String name = rs.getString("name"); 
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                
                // If you don't have a status column yet, default to "Active"
                String status = "Active"; 

                list.add(new SubscriberDTO(id, name, phone, email, status));
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }

    // ============================================
    // TEAMMATE'S CODE (From main) - Login Checks
    // ============================================
    public static boolean checkLogin(String username, String password) throws Exception {
        String sql = "SELECT 1 FROM subscribers WHERE username=? AND password=? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static String getEmailByUsername(String username) throws Exception {
        String sql = "SELECT email FROM subscribers WHERE username=? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("email");
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }
}