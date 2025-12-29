package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
}
