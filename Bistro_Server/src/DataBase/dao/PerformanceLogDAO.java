package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

}
