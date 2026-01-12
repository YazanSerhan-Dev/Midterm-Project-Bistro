package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.ReportDTO;

public class ReportDAO {

	public static List<ReportDTO> getPerformanceReport(int month, int year) throws Exception {
        List<ReportDTO> list = new ArrayList<>();
        String sql = """
            SELECT 
                report_date, 
                SUM(late_minutes) as total_late, 
                SUM(overstay_minutes) as total_overstay 
            FROM performance_log 
            WHERE MONTH(report_date) = ? 
              AND YEAR(report_date) = ?
            GROUP BY report_date
            ORDER BY report_date ASC
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReportDTO(
                        rs.getDate("report_date").toString(),
                        rs.getInt("total_late"),
                        rs.getInt("total_overstay")
                    ));
                }
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }

    // 2. Update getSubscriberActivityReport to accept params
    public static List<ReportDTO> getSubscriberActivityReport(int month, int year) throws Exception {
        List<ReportDTO> list = new ArrayList<>();
        String sql = """
            SELECT 
                DATE(activity_date) as act_date,
                COUNT(reservation_id) as total_res,
                COUNT(waiting_id) as total_wait
            FROM user_activity
            WHERE subscriber_username IS NOT NULL
              AND MONTH(activity_date) = ?
              AND YEAR(activity_date) = ?
            GROUP BY act_date
            ORDER BY act_date ASC
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReportDTO(
                        rs.getDate("act_date").toString(),
                        rs.getInt("total_res"),
                        rs.getInt("total_wait")
                    ));
                }
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }}