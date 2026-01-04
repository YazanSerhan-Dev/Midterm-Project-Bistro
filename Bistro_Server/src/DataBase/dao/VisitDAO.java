package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

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


}
