package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.CurrentDinersDTO;

public class VisitDAO {

    public static void insertVisit(
            int activityId, String tableId,
            Timestamp start, Timestamp end) throws Exception {

        String sql = """
            INSERT INTO visit
            (activity_id, table_id, actual_start_time, actual_end_time)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setString(2, tableId);
            ps.setTimestamp(3, start);
            ps.setTimestamp(4, end);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static void insertVisit(Connection conn, int activityId, String tableId) throws Exception {

        String sql = """
            INSERT INTO visit (activity_id, table_id, actual_start_time, actual_end_time)
            VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR))
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setString(2, tableId);
            ps.executeUpdate();
        }
    }
    
    public static List<CurrentDinersDTO> getActiveDiners() {
        List<CurrentDinersDTO> list = new ArrayList<>();
        
        String sql = """
            SELECT 
                v.table_id, 
                v.actual_start_time, 
                r.num_of_customers,
                s.name AS subscriber_name,
                ua.guest_email  -- ✅ Valid column from user_activity
            FROM visit v
            JOIN user_activity ua ON v.activity_id = ua.activity_id
            LEFT JOIN reservation r ON ua.reservation_id = r.reservation_id
            LEFT JOIN subscribers s ON ua.subscriber_username = s.username
            WHERE v.actual_end_time > NOW()
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = null;

        try {
            pc = pool.getConnection();
            Connection conn = pc.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    // 1. Parse Table Number
                    int tableNum = 0;
                    String tIdStr = rs.getString("table_id"); 
                    try {
                        tableNum = Integer.parseInt(tIdStr.replaceAll("\\D", ""));
                    } catch (Exception e) { tableNum = 0; }

                    // 2. Determine Name (Subscriber Name OR Guest Email)
                    String subName = rs.getString("subscriber_name");
                    String gstEmail = rs.getString("guest_email");
                    
                    String displayName = "Guest";
                    
                    if (subName != null && !subName.isBlank()) {
                        displayName = subName; // Display Subscriber Name
                    } else if (gstEmail != null && !gstEmail.isBlank()) {
                        displayName = gstEmail; // Display Guest Email
                    }

                    // 3. Other fields
                    int count = rs.getInt("num_of_customers");
                    Timestamp ts = rs.getTimestamp("actual_start_time");
                    String timeStr = (ts != null) ? ts.toLocalDateTime().toLocalTime().toString() : "-";

                    list.add(new CurrentDinersDTO(tableNum, displayName, count, timeStr, "Seated"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in VisitDAO.getActiveDiners: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (pc != null) pool.releaseConnection(pc);
        }
        return list;
    }
}