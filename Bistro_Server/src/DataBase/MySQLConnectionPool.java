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
 * Reuses connections and periodically closes idle ones.
 */
public class MySQLConnectionPool {

    // ---------- DB CONFIG
    private static final String URL = "jdbc:mysql://localhost:3306/bistro";
    private static final String USER = "root";
    private static final String PASSWORD = "Yazan12@"; 

    // ---------- POOL CONFIG ----------
    /** Maximum number of pooled connections that can be cached. */
    private static final int  MAX_POOL_SIZE      = 10;
    /** Idle timeout – how long a connection may sit unused before cleanup (ms). */
    private static final long MAX_IDLE_MILLIS    = 30_000L;  // 30s idle timeout
    /** How often the cleanup task runs (seconds). */
    private static final long CLEANUP_PERIOD_SEC = 10L;      // run cleanup every 10s

    // ---------- SINGLETON INSTANCE ----------
    private static MySQLConnectionPool instance;

    /**
     * Returns the single instance of the pool (lazy-loaded Singleton).
     */
    public static synchronized MySQLConnectionPool getInstance() {
        if (instance == null) {
            instance = new MySQLConnectionPool();
        }
        return instance;
    }

    // ---------- INTERNAL STATE ----------
    /** Queue of pooled connections that are currently free to use. */
    private final BlockingQueue<PooledConnection> pool;
    /** Background task that periodically removes idle connections. */
    private final ScheduledExecutorService cleaner;

    /**
     * Private constructor – only getInstance() can create the pool.
     * Initializes the queue and schedules the periodic cleanup job.
     */
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
     * Gets a pooled connection from the queue, or creates a new one if needed.
     *
     * @return a PooledConnection ready to use (wrapped JDBC Connection)
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
            // Reusing existing connection – update last-used timestamp
            pConn.touch();
        }

        return pConn;
    }

    /**
     * Returns a connection wrapper to the pool after use.
     * If the pool is full, the physical connection is closed instead.
     */
    public void releaseConnection(PooledConnection pConn) {
        if (pConn == null) {
            return;
        }

        // Mark as recently used before putting back in the pool
        pConn.touch();

        boolean offered = pool.offer(pConn);
        if (!offered) {
            // Pool is full -> close it instead of blocking
            pConn.closePhysicalConnection();
        }
    }

    /**
     * Periodic cleanup: closes connections that have been idle for too long.
     * Runs automatically according to CLEANUP_PERIOD_SEC.
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
     * Shuts down the pool: stops the cleaner thread and closes all pooled connections.
     * Optional – can be called when the server is shutting down.
     */
    public void shutdown() {
        cleaner.shutdown();
        for (PooledConnection pConn : pool) {
            pConn.closePhysicalConnection();
        }
        pool.clear();
    }
}






