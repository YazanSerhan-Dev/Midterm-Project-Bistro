package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class PerformanceLogDAO {

    public static void insertLog(
            int visitId,
            int lateMinutes,
            int overstayMinutes,
            Date reportDate) throws Exception {

        String sql = """
            INSERT INTO performance_log
            (visit_id, late_minutes, overstay_minutes, report_date)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.setInt(2, lateMinutes);
            ps.setInt(3, overstayMinutes);
            ps.setDate(4, reportDate);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static void insertAutoReservedNotConfirmed(Connection conn, int visitId) throws Exception {
        String sql = """
            INSERT INTO performance_log (visit_id, late_minutes, overstay_minutes, report_date)
            VALUES (?, -999, 0, CURDATE())
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.executeUpdate();
        }
    }

    public static boolean isAutoReservedNotConfirmed(Connection conn, int visitId) throws Exception {
        String sql = """
            SELECT 1
            FROM performance_log
            WHERE visit_id = ? AND late_minutes = -999
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void confirmAutoReserved(Connection conn, int visitId) throws Exception {
        String sql = """
            UPDATE performance_log
            SET late_minutes = 0
            WHERE visit_id = ? AND late_minutes = -999
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.executeUpdate();
        }
    }

    public static void deleteByVisitId(Connection conn, int visitId) throws Exception {
        String sql = "DELETE FROM performance_log WHERE visit_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.executeUpdate();
        }
    }
    
    public static int generateMonthlyReport() {
        int rowsAffected = 0;
        
        // This query selects visits from the LAST month that are NOT in performance_log yet
        String sql = """
            INSERT INTO performance_log (visit_id, late_minutes, overstay_minutes, report_date)
            SELECT 
                v.visit_id,
                
                -- Calculate Late Minutes (Only for Reservations) --
                CASE 
                    WHEN r.reservation_time IS NOT NULL AND v.actual_start_time > r.reservation_time
                    THEN TIMESTAMPDIFF(MINUTE, r.reservation_time, v.actual_start_time)
                    ELSE 0 
                END AS late_calc,
                
                -- Calculate Overstay Minutes (Duration > 120 mins) --
                CASE 
                    WHEN TIMESTAMPDIFF(MINUTE, v.actual_start_time, v.actual_end_time) > 120
                    THEN TIMESTAMPDIFF(MINUTE, v.actual_start_time, v.actual_end_time) - 120
                    ELSE 0 
                END AS overstay_calc,
                
                CURRENT_DATE
                
            FROM visit v
            JOIN user_activity ua ON v.activity_id = ua.activity_id
            LEFT JOIN reservation r ON ua.reservation_id = r.reservation_id
            WHERE 
                -- 1. Visit is completed
                v.actual_end_time IS NOT NULL
                
                -- 2. Visit happened in the PREVIOUS month (relative to today)
                AND MONTH(v.actual_end_time) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH)
                AND YEAR(v.actual_end_time) = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)
                
                -- 3. Avoid duplicates: Only insert if not already logged
                AND v.visit_id NOT IN (SELECT visit_id FROM performance_log)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = null;

        try {
            pc = pool.getConnection();
            Connection conn = pc.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                rowsAffected = ps.executeUpdate();
            }
        } catch (Exception e) {
            System.out.println("[Report] Error generating monthly report: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (pc != null) pool.releaseConnection(pc);
        }
        
        return rowsAffected;
    }
}

   

