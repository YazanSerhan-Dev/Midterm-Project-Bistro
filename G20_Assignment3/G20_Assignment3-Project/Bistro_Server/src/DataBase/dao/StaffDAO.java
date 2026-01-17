package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
/**
 * Data Access Object for staff management.
 * <p>
 * Handles persistence and authentication logic
 * for staff members such as agents and managers.
 * Responsible for inserting staff records and
 * validating login credentials.
 */
public class StaffDAO {
	/**
	 * Inserts a new staff member into the system.
	 * <p>
	 * Stores login credentials, role, and full name.
	 * Used during system setup or staff management operations.
	 *
	 * @param username unique staff username
	 * @param password staff password
	 * @param role staff role (e.g., AGENT, MANAGER)
	 * @param fullName staff full name
	 * @throws Exception on database error
	 */
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

    // ✅ NEW: login check for staff + return role ("AGENT"/"MANAGER"), null if invalid
    /**
     * Validates staff login credentials and retrieves the staff role.
     * <p>
     * Used for staff authentication during login.
     * Returns the role if credentials are valid, or null otherwise.
     *
     * @param username staff username
     * @param password staff password
     * @return staff role if login is valid, null if invalid
     * @throws Exception on database error
     */
    public static String checkLoginAndGetRole(String username, String password) throws Exception {
        String sql = "SELECT staff_role FROM staff WHERE username=? AND password=? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("staff_role");
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }
}

