package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class WaitingListDAO {

    public static void insertWaiting(
            int numOfCustomers,
            Timestamp requestTime,
            String status,
            String confirmationCode) throws Exception {

        String sql = """
            INSERT INTO waiting_list
            (num_of_customers, request_time, status, confirmation_code)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numOfCustomers);
            ps.setTimestamp(2, requestTime);
            ps.setString(3, status);
            ps.setString(4, confirmationCode);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
}
