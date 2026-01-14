package DataBase.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.ProfileDTO;
import common.dto.SubscriberDTO;

public class SubscriberDAO {

    public static void insertSubscriber(
            String username, String password, String name,
            String phone, String email,
           Date birthDate) throws Exception {
    	
    	String memberCode = generateNextId("member_code", "MEM");
        String barcode = generateNextId("barcode_data", "BAR");

        // 2. If birthDate is missing from UI, default to today (prevents crash)
        if (birthDate == null) {
            birthDate = new Date(System.currentTimeMillis());
        }

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
    
    private static String generateNextId(String columnName, String prefix) throws Exception {
        String sql = "SELECT " + columnName + " FROM subscribers";
        
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();
        
        int maxId = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String val = rs.getString(1); // e.g., "MEM12"
                if (val != null && val.startsWith(prefix)) {
                    try {
                        // Extract the number part ("12")
                        String numPart = val.substring(prefix.length());
                        int num = Integer.parseInt(numPart);
                        if (num > maxId) {
                            maxId = num;
                        }
                    } catch (NumberFormatException ignored) {
                        // Skip if it's not a valid number format
                    }
                }
            }
        } finally {
            pool.releaseConnection(pc);
        }

        // Return the next number (e.g., "MEM13")
        return prefix + (maxId + 1);
    }

    // ============================================
    // YOUR CODE (From HEAD) - Get List for Agent
    // ============================================
 // In getAllSubscribers() method:

    public static List<SubscriberDTO> getAllSubscribers() throws Exception {
        List<SubscriberDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM subscribers"; 

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                
                // ✅ Fetch birth_date (handling potential nulls if necessary)
                String birthDate = rs.getString("birth_date"); 
                if (birthDate == null) birthDate = "";

                // ✅ Pass birthDate to the new DTO constructor
                list.add(new SubscriberDTO(username, name, phone, email, birthDate));
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
    
    public static String getPhoneByUsername(String username) throws Exception {
        String sql = "SELECT phone FROM subscribers WHERE username = ? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("phone");
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static ProfileDTO getProfileByMemberCode(String memberCode) throws Exception {
        String sql = "SELECT member_code, name, phone, email, barcode_data " +
                     "FROM subscribers WHERE member_code = ? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new ProfileDTO(
                    rs.getString("member_code"), // maps to ProfileDTO.memberNumber
                    rs.getString("name"),        // maps to ProfileDTO.fullName
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("barcode_data")
                );
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static boolean updateProfileByMemberCode(ProfileDTO dto) throws Exception {
        String sql = "UPDATE subscribers SET name = ?, phone = ?, email = ? " +
                     "WHERE member_code = ?";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dto.getFullName());      // -> name
            ps.setString(2, dto.getPhone());
            ps.setString(3, dto.getEmail());
            ps.setString(4, dto.getMemberNumber());  // -> member_code

            return ps.executeUpdate() == 1;

        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static String getMemberCodeByUsername(String username) throws Exception {
        String sql = "SELECT member_code FROM subscribers WHERE username = ? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("member_code");
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static String getUsernameByBarcodeData(String barcodeData) throws Exception {
        String sql = "SELECT username FROM subscribers WHERE barcode_data = ? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcodeData);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("username");
            }

        } finally {
            pool.releaseConnection(pc);
        }
    }


}