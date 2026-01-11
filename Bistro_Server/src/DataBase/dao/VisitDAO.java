package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
            VALUES (?, ?, NOW(), NULL)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setString(2, tableId);
            ps.executeUpdate();
        }
    }
    
    public static boolean existsVisitForWaitingId(int waitingId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.waiting_id = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeQuery().next();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static boolean existsVisitForWaitingId(Connection conn, int waitingId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.waiting_id = ?
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeQuery().next();
        }
    }

    public static boolean existsVisitForReservationId(int reservationId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.reservation_id = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            return ps.executeQuery().next();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static boolean existsVisitForReservationId(Connection conn, int reservationId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.reservation_id = ?
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            return ps.executeQuery().next();
        }
    }

}
