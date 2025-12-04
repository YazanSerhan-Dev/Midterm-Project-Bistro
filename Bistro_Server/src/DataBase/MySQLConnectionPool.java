package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Singleton MySQL connection pool.
 * Reuses connections and closes idle ones.
 */
public class MySQLConnectionPool {

    // ---------- DB CONFIG: change to your DB if needed ----------
    private static final String URL = "jdbc:mysql://localhost:3306/bistrodb";
    private static final String USER = "root";
    private static final String PASSWORD = "Yazan12@"; 

    // ---------- POOL CONFIG ----------
    private static final int  MAX_POOL_SIZE      = 10;       // max cached connections
    private static final long MAX_IDLE_MILLIS    = 30_000L;  // 30s idle timeout
    private static final long CLEANUP_PERIOD_SEC = 10L;      // run cleanup every 10s

    // ---------- SINGLETON INSTANCE ----------
    private static MySQLConnectionPool instance;

    public static synchronized MySQLConnectionPool getInstance() {
        if (instance == null) {
            instance = new MySQLConnectionPool();
        }
        return instance;
    }

    // ---------- INTERNAL STATE ----------
    private final BlockingQueue<PooledConnection> pool;
    private final ScheduledExecutorService cleaner;

    // Private constructor -> only getInstance() can create
    private MySQLConnectionPool() {
        pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

        cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleAtFixedRate(
                this::cleanupIdleConnections,
                CLEANUP_PERIOD_SEC,
                CLEANUP_PERIOD_SEC,
                TimeUnit.SECONDS
        );
    }

    /**
     * Get a connection wrapper from the pool.
     * If the pool is empty, create a new physical connection.
     */
    public PooledConnection getConnection() {
        // Try to reuse from queue
        PooledConnection pConn = pool.poll();

        if (pConn == null) {
            // Queue empty -> create new physical connection
            try {
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                pConn = new PooledConnection(conn);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create DB connection", e);
            }
        } else {
            // Reusing existing connection
            pConn.touch();
        }

        return pConn;
    }

    /**
     * Return a connection wrapper to the pool after using it.
     * If the pool is already full, close the connection instead.
     */
    public void releaseConnection(PooledConnection pConn) {
        if (pConn == null) {
            return;
        }

        pConn.touch(); // mark as used

        boolean offered = pool.offer(pConn);
        if (!offered) {
            // Pool is full -> close it instead of blocking
            pConn.closePhysicalConnection();
        }
    }

    /**
     * Periodically closes connections that were idle for too long.
     */
    private void cleanupIdleConnections() {
        long now = System.currentTimeMillis();

        Iterator<PooledConnection> it = pool.iterator();
        while (it.hasNext()) {
            PooledConnection pConn = it.next();
            if (now - pConn.getLastUsed() > MAX_IDLE_MILLIS) {
                it.remove();
                pConn.closePhysicalConnection();
            }
        }
    }

    /**
     * Optional: call this when shutting down the server.
     */
    public void shutdown() {
        cleaner.shutdown();
        for (PooledConnection pConn : pool) {
            pConn.closePhysicalConnection();
        }
        pool.clear();
    }
}


