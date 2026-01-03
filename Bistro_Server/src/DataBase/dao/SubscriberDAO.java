package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

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

    // ✅ NEW: login check for subscriber
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
