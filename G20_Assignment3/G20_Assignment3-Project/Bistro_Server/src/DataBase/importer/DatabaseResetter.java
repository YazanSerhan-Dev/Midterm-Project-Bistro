package DataBase.importer;

import java.sql.Connection;
import java.sql.Statement;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

/**
 * Utility class responsible for clearing all database tables
 * before running data import processes.
 * <p>
 * The reset is performed using TRUNCATE statements in a safe order
 * while temporarily disabling foreign key checks.
 * <p>
 * Intended for testing, development, and initial data loading only.
 */
public class DatabaseResetter {
	/**
	 * Resets the entire database content by truncating all tables.
	 * <p>
	 * Process:
	 * <ul>
	 *   <li>Disables foreign key constraints</li>
	 *   <li>Truncates tables from dependent to base entities</li>
	 *   <li>Re-enables foreign key constraints</li>
	 * </ul>
	 *
	 * @throws Exception if a database error occurs
	 */
    public static void resetDatabase() throws Exception {

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = pool.getConnection();
        Connection conn = pConn.getConnection();

        try (Statement stmt = conn.createStatement()) {

            // Disable FK checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // ===== Child tables (depend on visit) =====
            stmt.execute("TRUNCATE performance_log");
            stmt.execute("TRUNCATE bill");

            // ===== Visit & activity =====
            stmt.execute("TRUNCATE visit");
            stmt.execute("TRUNCATE user_activity");

            // ===== Business flow =====
            stmt.execute("TRUNCATE reservation");
            stmt.execute("TRUNCATE waiting_list");

            // ===== Master data =====
            stmt.execute("TRUNCATE opening_hours");
            stmt.execute("TRUNCATE restaurant_table");
            stmt.execute("TRUNCATE subscribers");
            stmt.execute("TRUNCATE staff");

            // Enable FK checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("✅ Database reset completed");

        } finally {
            pool.releaseConnection(pConn);
        }
    }
}
