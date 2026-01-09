package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class RestaurantTableDAO {

    public static void insertTable(
            String tableId,
            int seats,
            String status) throws Exception {

        String sql = """
            INSERT INTO restaurant_table
            (table_id, num_of_seats, status)
            VALUES (?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            ps.setInt(2, seats);
            ps.setString(3, status);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static int getTotalSeatsAvailable() throws Exception {
        String sql =
                "SELECT COALESCE(SUM(num_of_seats),0) AS total " +
                "FROM restaurant_table " +
                "WHERE status = ?";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
        	ps.setString(1, "FREE"); // change this if your status values differ

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
                return 0;
            }

        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static int getTotalSeats() throws Exception {
        String sql =
                "SELECT COALESCE(SUM(num_of_seats),0) AS total " +
                "FROM restaurant_table";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt("total") : 0;

        } finally {
            pool.releaseConnection(pc);
        }
    }


    public static String allocateFreeTable(Connection conn, int numCustomers) throws Exception {
        String selectSql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'FREE' AND num_of_seats >= ?
            ORDER BY num_of_seats ASC
            LIMIT 1
        """;

        String updateSql = """
            UPDATE restaurant_table
            SET status = 'OCCUPIED'
            WHERE table_id = ? AND status = 'FREE'
        """;

        String tableId = null;

        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, numCustomers);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tableId = rs.getString("table_id");
            }
        }

        if (tableId == null) return null;

        try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
            ps2.setString(1, tableId);
            int updated = ps2.executeUpdate();
            if (updated != 1) return null; // someone took it
        }

        return tableId;
    }
    
    public static int freeTablesForExpiredReservations() throws Exception {

        String sql = """
            UPDATE restaurant_table t
            JOIN visit v ON v.table_id = t.table_id
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            JOIN reservation r ON r.reservation_id = ua.reservation_id
            SET t.status = 'FREE'
            WHERE r.status = 'EXPIRED'
              AND t.status = 'OCCUPIED'
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int getMaxTableSeats() throws Exception {
        String sql = "SELECT COALESCE(MAX(num_of_seats),0) AS maxSeats FROM restaurant_table";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt("maxSeats");
            return 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
 // === Option-1: REAL reservation hold using restaurant_table columns ===

    public static String reserveFreeTableForReservation(Connection conn, int numCustomers, int reservationId, int holdMinutes) throws Exception {

        // pick smallest FREE that fits
        String selectSql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'FREE' AND num_of_seats >= ?
            ORDER BY num_of_seats ASC
            LIMIT 1
        """;

        // reserve it for THIS reservation (atomic)
        String updateSql = """
            UPDATE restaurant_table
            SET status = 'RESERVED',
                reserved_for_reservation_id = ?,
                reserved_until = DATE_ADD(NOW(), INTERVAL ? MINUTE)
            WHERE table_id = ?
              AND status = 'FREE'
        """;

        String tableId = null;

        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, numCustomers);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tableId = rs.getString("table_id");
            }
        }

        if (tableId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, holdMinutes);
            ps.setString(3, tableId);

            int updated = ps.executeUpdate();
            if (updated == 0) return null; // race: someone took it
        }

        return tableId;
    }

    public static String getReservedTableForReservation(Connection conn, int reservationId) throws Exception {
        String sql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'RESERVED'
              AND reserved_for_reservation_id = ?
              AND reserved_until > NOW()
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("table_id");
                return null;
            }
        }
    }

    public static boolean occupyReservedTableForReservation(Connection conn, String tableId, int reservationId) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'OCCUPIED',
                reserved_for_reservation_id = NULL,
                reserved_until = NULL
            WHERE table_id = ?
              AND status = 'RESERVED'
              AND reserved_for_reservation_id = ?
              AND reserved_until IS NOT NULL
              AND reserved_until > NOW()
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            ps.setInt(2, reservationId);
            return ps.executeUpdate() == 1;
        }
    }

    public static int releaseExpiredReservedTables(Connection conn) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'FREE',
                reserved_for_reservation_id = NULL,
                reserved_until = NULL
            WHERE status = 'RESERVED'
              AND reserved_until IS NOT NULL
              AND reserved_until <= NOW()
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }


    public static boolean freeTable(Connection conn, String tableId) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'FREE'
            WHERE table_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            return ps.executeUpdate() > 0;
        }
    }

    public static int releaseExpiredReservedTables() throws Exception {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            return releaseExpiredReservedTables(conn);
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static String reserveFreeTableForWaitingReturnTableId(Connection conn, int waitingId, int people) throws Exception {
        // 1) pick best-fit FREE table that is not already reserved
        String select = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'FREE'
              AND num_of_seats >= ?
              AND reserved_for_reservation_id IS NULL
              AND reserved_for_waiting_id IS NULL
            ORDER BY num_of_seats ASC
            LIMIT 1
            FOR UPDATE
        """;

        String update = """
            UPDATE restaurant_table
            SET status = 'RESERVED',
                reserved_for_waiting_id = ?,
                reserved_until = DATE_ADD(NOW(), INTERVAL 15 MINUTE)
            WHERE table_id = ?
              AND status = 'FREE'
              AND reserved_for_reservation_id IS NULL
              AND reserved_for_waiting_id IS NULL
        """;

        String tableId = null;

        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, people);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tableId = rs.getString("table_id");
            }
        }

        if (tableId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, waitingId);
            ps.setString(2, tableId);
            int updated = ps.executeUpdate();
            return (updated == 1) ? tableId : null;
        }
    }

    public static String getReservedTableForWaiting(Connection conn, int waitingId) throws Exception {
        String sql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'RESERVED'
              AND reserved_for_waiting_id = ?
              AND reserved_until IS NOT NULL
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("table_id");
                return null;
            }
        }
    }

    public static boolean occupyReservedTableForWaiting(Connection conn, String tableId, int waitingId) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'OCCUPIED',
                reserved_for_waiting_id = NULL,
                reserved_until = NULL
            WHERE table_id = ?
              AND status = 'RESERVED'
              AND reserved_for_waiting_id = ?
              AND reserved_until IS NOT NULL
              AND reserved_until >= NOW()
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            ps.setInt(2, waitingId);
            return ps.executeUpdate() == 1;
        }
    }

    public static int releaseExpiredReservedTablesForWaiting() throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'FREE',
                reserved_for_waiting_id = NULL,
                reserved_until = NULL
            WHERE status = 'RESERVED'
              AND reserved_for_waiting_id IS NOT NULL
              AND reserved_until IS NOT NULL
              AND reserved_until < NOW()
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }


}
