package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
}
