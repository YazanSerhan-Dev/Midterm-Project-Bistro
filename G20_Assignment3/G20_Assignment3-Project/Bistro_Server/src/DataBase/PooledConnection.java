package DataBase;

import java.sql.Connection;
import java.sql.SQLException;
/**
 * Creates a new pooled connection wrapper.
 *
 */
public class PooledConnection {

    // The real JDBC connection to MySQL
    private final Connection conn;

    // Last time (in millis) this connection was used
    private long lastUsed;

    public PooledConnection(Connection conn) {
        this.conn = conn;
        touch(); // Mark as just used
    }

    /**
     * Returns the underlying JDBC Connection.
     * Updates the last-used timestamp to reflect active usage.
     *
     * @return active JDBC Connection
     */

    public Connection getConnection() {
        touch();
        return conn;
    }

    /**
     * Update last-used timestamp to "now".
     */
    public void touch() {
        lastUsed = System.currentTimeMillis();
    }

    /**
     * Get the last-used timestamp (in millis).
     */
    public long getLastUsed() {
        return lastUsed;
    }

    /**
     * Physically close the DB connection.
     * Only the connection pool should call this.
     */
    public void closePhysicalConnection() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace(); // or log it
        }
    }
}



