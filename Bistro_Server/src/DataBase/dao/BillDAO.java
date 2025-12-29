package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class BillDAO {

    public static void insertBill(
            int visitId, double amount,
            String discount, String paid) throws Exception {

        String sql = """
            INSERT INTO bill
            (visit_id, total_amount, is_subscriber_discount, is_paid)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.setDouble(2, amount);
            ps.setString(3, discount);
            ps.setString(4, paid);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
}
