package DataBase;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a real JDBC Connection and stores when it was last used.
 * The connection pool uses this information to close idle connections.
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
     * Also updates the last-used timestamp.
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



