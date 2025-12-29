package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class StaffDAO {

    public static void insertStaff(String username, String password,
                                   String role, String fullName) throws Exception {

        String sql = """
            INSERT INTO staff (username, password, staff_role, full_name)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
}
