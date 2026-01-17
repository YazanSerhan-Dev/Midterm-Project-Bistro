package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.ReportDTO;
/**
 * Data Access Object for generating management reports.
 * <p>
 * Provides aggregated monthly data for:
 * <ul>
 *   <li>Performance reports (late arrivals, overstays)</li>
 *   <li>Subscriber activity reports (reservations vs waiting list)</li>
 * </ul>
 * Data is returned in a daily aggregated form for charting and analysis.
 */
public class ReportDAO {

    // 1. Performance Report
	/**
	 * Retrieves a monthly performance report.
	 * <p>
	 * Aggregates late arrival minutes and overstay minutes per day
	 * based on records stored in the performance_log table.
	 *
	 * @param month report month (1–12)
	 * @param year  report year (e.g. 2025)
	 * @return list of daily performance report entries
	 * @throws Exception if a database error occurs
	 */
    public static List<ReportDTO> getPerformanceReport(int month, int year) throws Exception {
        List<ReportDTO> list = new ArrayList<>();
        String sql = """
            SELECT 
                DATE(report_date) as r_date, 
                SUM(late_minutes) as total_late, 
                SUM(overstay_minutes) as total_overstay 
            FROM performance_log 
            WHERE MONTH(report_date) = ? 
              AND YEAR(report_date) = ?
            GROUP BY r_date
            ORDER BY r_date ASC
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReportDTO dto = new ReportDTO();
                    dto.setDate(rs.getString("r_date"));
                    // Set specific performance fields
                    dto.setTotalLate(rs.getInt("total_late"));
                    dto.setTotalOverstay(rs.getInt("total_overstay"));
                    list.add(dto);
                }
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }

    // 2. Activity Report
    /**
     * Retrieves a monthly subscriber activity report.
     * <p>
     * Counts the number of reservations and waiting-list entries
     * per day based on user activity records.
     *
     * @param month report month (1–12)
     * @param year  report year (e.g. 2025)
     * @return list of daily activity report entries
     * @throws Exception if a database error occurs
     */
    public static List<ReportDTO> getSubscriberActivityReport(int month, int year) throws Exception {
        List<ReportDTO> list = new ArrayList<>();
        // Note: Using LEFT JOIN or just checking tables. 
        // For simplicity, we count from user_activity.
        String sql = """
            SELECT 
                DATE(activity_date) as act_date,
                COUNT(CASE WHEN reservation_id IS NOT NULL THEN 1 END) as total_res,
                COUNT(CASE WHEN waiting_id IS NOT NULL THEN 1 END) as total_wait
            FROM user_activity
            WHERE MONTH(activity_date) = ?
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
                    ReportDTO dto = new ReportDTO();
                    dto.setDate(rs.getString("act_date"));
                    // Set specific activity fields
                    dto.setTotalReservations(rs.getInt("total_res"));
                    dto.setTotalWaiting(rs.getInt("total_wait"));
                    list.add(dto);
                }
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }
}