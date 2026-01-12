package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import java.util.ArrayList;
import java.util.List;
import common.dto.RestaurantTableDTO;

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

    public static List<RestaurantTableDTO> getAllTables() throws Exception {
        List<RestaurantTableDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_table ORDER BY table_id";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new RestaurantTableDTO(
                    rs.getString("table_id"),
                    rs.getInt("num_of_seats"),
                    rs.getString("status")
                ));
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }

    // 2. Delete Table
    public static boolean deleteTable(String tableId) throws Exception {
        String sql = "DELETE FROM restaurant_table WHERE table_id = ?";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    // 3. Update Table (Seats)
    public static boolean updateTableSeats(String tableId, int newSeats) throws Exception {
        String sql = "UPDATE restaurant_table SET num_of_seats = ? WHERE table_id = ?";
        
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newSeats);
            ps.setString(2, tableId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }
}


