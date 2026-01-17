package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.RestaurantTableDTO;
/**
 * Data Access Object for restaurant table management.
 * <p>
 * Responsible for table inventory operations including:
 * allocation, reservation (hold), occupation, release,
 * and feasibility planning for seating reservations.
 * <p>
 * Supports single-table and multi-table allocation,
 * reservation waiting logic, and planning simulations
 * without modifying table status.
 */
public class RestaurantTableDAO {
	/**
	 * Inserts or updates a restaurant table definition.
	 * <p>
	 * If the table already exists, only the seat count is updated.
	 *
	 * @param tableId unique table identifier
	 * @param seats number of seats at the table
	 * @param status initial table status (FREE / RESERVED / OCCUPIED)
	 * @throws Exception on database error
	 */
	public static void insertTable(String tableId, int seats, String status) throws Exception {
        String sql = """
            INSERT INTO restaurant_table (table_id, num_of_seats, status)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE num_of_seats = VALUES(num_of_seats)
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
	/**
	 * Calculates the total number of seats currently available (FREE tables only).
	 *
	 * @return total free seats
	 * @throws Exception on query failure
	 */
    public static int getTotalSeatsAvailable() throws Exception {
        String sql =
                "SELECT COALESCE(SUM(num_of_seats),0) AS total " +
                "FROM restaurant_table " +
                "WHERE status = ?";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
        	ps.setString(1, "FREE"); // change this if your status values differ

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
                return 0;
            }

        } finally {
            pool.releaseConnection(pc);
        }
    }
    /**
     * Calculates the total seating capacity of the restaurant.
     *
     * @return total number of seats across all tables
     * @throws Exception on query failure
     */
    public static int getTotalSeats() throws Exception {
        String sql =
                "SELECT COALESCE(SUM(num_of_seats),0) AS total " +
                "FROM restaurant_table";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt("total") : 0;

        } finally {
            pool.releaseConnection(pc);
        }
    }

    /**
     * Allocates a single FREE table that can fit the given number of customers.
     * <p>
     * Chooses the smallest suitable table (best-fit) and marks it OCCUPIED.
     *
     * @param conn active database connection
     * @param numCustomers party size
     * @return allocated table id, or null if none available
     * @throws Exception on database error
     */
    public static String allocateFreeTable(Connection conn, int numCustomers) throws Exception {
        String selectSql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'FREE' AND num_of_seats >= ?
            ORDER BY num_of_seats ASC
            LIMIT 1
        """;

        String updateSql = """
            UPDATE restaurant_table
            SET status = 'OCCUPIED'
            WHERE table_id = ? AND status = 'FREE'
        """;

        String tableId = null;

        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, numCustomers);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tableId = rs.getString("table_id");
            }
        }

        if (tableId == null) return null;

        try (PreparedStatement ps2 = conn.prepareStatement(updateSql)) {
            ps2.setString(1, tableId);
            int updated = ps2.executeUpdate();
            if (updated != 1) return null; // someone took it
        }

        return tableId;
    }
    
 // === Option-1: REAL reservation hold using restaurant_table columns ===
    /**
     * Reserves a FREE table for a reservation for a limited hold time.
     * <p>
     * Marks the table as RESERVED and links it to the reservation id.
     *
     * @param conn active database connection
     * @param numCustomers party size
     * @param reservationId reservation identifier
     * @param holdMinutes reservation hold duration
     * @return reserved table id, or null if none available
     * @throws Exception on database error
     */
    public static String reserveFreeTableForReservation(Connection conn, int numCustomers, int reservationId, int holdMinutes) throws Exception {

        // pick smallest FREE that fits
        String selectSql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'FREE' AND num_of_seats >= ?
            ORDER BY num_of_seats ASC
            LIMIT 1
        """;

        // reserve it for THIS reservation (atomic)
        String updateSql = """
            UPDATE restaurant_table
            SET status = 'RESERVED',
                reserved_for_reservation_id = ?,
                reserved_until = DATE_ADD(NOW(), INTERVAL ? MINUTE)
            WHERE table_id = ?
              AND status = 'FREE'
        """;

        String tableId = null;

        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, numCustomers);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tableId = rs.getString("table_id");
            }
        }

        if (tableId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, reservationId);
            ps.setInt(2, holdMinutes);
            ps.setString(3, tableId);

            int updated = ps.executeUpdate();
            if (updated == 0) return null; // race: someone took it
        }

        return tableId;
    }

    public static String getReservedTableForReservation(Connection conn, int reservationId) throws Exception {
        String sql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'RESERVED'
              AND reserved_for_reservation_id = ?
              AND reserved_until > NOW()
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("table_id");
                return null;
            }
        }
    }

    public static int releaseExpiredReservedTables(Connection conn) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'FREE',
                reserved_for_reservation_id = NULL,
                reserved_until = NULL
            WHERE status = 'RESERVED'
              AND reserved_until IS NOT NULL
              AND reserved_until <= NOW()
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }


    public static boolean freeTable(Connection conn, String tableId) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'FREE'
            WHERE table_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            return ps.executeUpdate() > 0;
        }
    }

    public static int releaseExpiredReservedTables() throws Exception {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            return releaseExpiredReservedTables(conn);
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static String reserveFreeTableForWaitingReturnTableId(Connection conn, int waitingId, int people) throws Exception {
        // 1) pick best-fit FREE table that is not already reserved
        String select = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'FREE'
              AND num_of_seats >= ?
              AND reserved_for_reservation_id IS NULL
              AND reserved_for_waiting_id IS NULL
            ORDER BY num_of_seats ASC
            LIMIT 1
            FOR UPDATE
        """;

        String update = """
            UPDATE restaurant_table
            SET status = 'RESERVED',
                reserved_for_waiting_id = ?,
                reserved_until = DATE_ADD(NOW(), INTERVAL 15 MINUTE)
            WHERE table_id = ?
              AND status = 'FREE'
              AND reserved_for_reservation_id IS NULL
              AND reserved_for_waiting_id IS NULL
        """;

        String tableId = null;

        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, people);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) tableId = rs.getString("table_id");
            }
        }

        if (tableId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, waitingId);
            ps.setString(2, tableId);
            int updated = ps.executeUpdate();
            return (updated == 1) ? tableId : null;
        }
    }

    public static int releaseExpiredReservedTablesForWaiting() throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'FREE',
                reserved_for_waiting_id = NULL,
                reserved_until = NULL
            WHERE status = 'RESERVED'
              AND reserved_for_waiting_id IS NOT NULL
              AND reserved_until IS NOT NULL
              AND reserved_until < NOW()
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    private static class TableCandidate {
        final String tableId;
        final int seats;
        TableCandidate(String tableId, int seats) {
            this.tableId = tableId;
            this.seats = seats;
        }
    }

    private static List<TableCandidate> getFreeTables(Connection conn) throws Exception {
        String sql = """
            SELECT table_id, num_of_seats
            FROM restaurant_table
            WHERE status = 'FREE'
            ORDER BY num_of_seats DESC
        """;
        List<TableCandidate> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new TableCandidate(rs.getString(1), rs.getInt(2)));
            }
        }
        return out;
    }

    /**
     * DP: choose a subset with totalSeats >= needed and minimal waste (totalSeats - needed).
     * Tie-break: fewer tables.
     */
    private static List<TableCandidate> chooseBestFit(List<TableCandidate> free, int needed) {
        if (needed <= 0) return Collections.emptyList();
        if (free.isEmpty()) return null;

        int maxSum = 0;
        for (TableCandidate t : free) maxSum += Math.max(0, t.seats);
        if (maxSum < needed) return null;

        final int INF = 1_000_000;

        boolean[] dp = new boolean[maxSum + 1];
        int[] count = new int[maxSum + 1];
        int[] prevSum = new int[maxSum + 1];
        int[] prevIdx = new int[maxSum + 1];

        for (int s = 0; s <= maxSum; s++) {
            count[s] = INF;
            prevSum[s] = -1;
            prevIdx[s] = -1;
        }

        dp[0] = true;
        count[0] = 0;

        for (int i = 0; i < free.size(); i++) {
            int w = free.get(i).seats;
            if (w <= 0) continue;

            for (int s = maxSum - w; s >= 0; s--) {
                if (!dp[s]) continue;

                int ns = s + w;
                int newCount = count[s] + 1;

                // Update if ns not reachable or we found fewer tables for same sum
                if (!dp[ns] || newCount < count[ns]) {
                    dp[ns] = true;
                    count[ns] = newCount;
                    prevSum[ns] = s;
                    prevIdx[ns] = i;
                }
            }
        }

        // find best sum >= needed with minimal waste; tie-break fewer tables
        int bestSum = -1;
        for (int s = needed; s <= maxSum; s++) {
            if (!dp[s]) continue;
            if (bestSum == -1) {
                bestSum = s;
                continue;
            }
            int waste = s - needed;
            int bestWaste = bestSum - needed;
            if (waste < bestWaste) bestSum = s;
            else if (waste == bestWaste && count[s] < count[bestSum]) bestSum = s;
        }

        if (bestSum == -1) return null;

        // reconstruct
        List<TableCandidate> chosen = new ArrayList<>();
        int cur = bestSum;
        while (cur > 0) {
            int i = prevIdx[cur];
            int ps = prevSum[cur];
            if (i < 0 || ps < 0) break;
            chosen.add(free.get(i));
            cur = ps;
        }

        return chosen;
    }

    /**
     * Allocates one or more FREE tables for a reservation at check-in time.
     * <p>
     * First attempts a single-table allocation.
     * If not possible, uses a best-fit DP strategy
     * to allocate multiple tables with minimal seat waste.
     *
     * @param conn active database connection
     * @param reservationId reservation identifier
     * @param numCustomers party size
     * @return list of allocated table ids
     * @throws Exception if allocation fails (atomic operation)
     */
    public static List<String> allocateFreeTablesBestFit(Connection conn, int reservationId, int numCustomers) throws Exception {
        // 1) try single-table fast path first
        String single = allocateFreeTable(conn, numCustomers);
        if (single != null) {
            // store linkage so we can free all later by reservationId
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE restaurant_table
                SET reserved_for_reservation_id = ?
                WHERE table_id = ?
            """)) {
                ps.setInt(1, reservationId);
                ps.setString(2, single);
                int updated = ps.executeUpdate();
                if (updated != 1) {
                    // This should not happen, but if it does we must fail to force rollback
                    throw new Exception("Failed to link occupied table to reservation: " + single);
                }
            }
            List<String> list = new ArrayList<>();
            list.add(single);
            return list;
        }

        // 2) DP best-fit on all free tables
        List<TableCandidate> free = getFreeTables(conn);
        List<TableCandidate> chosen = chooseBestFit(free, numCustomers);
        if (chosen == null || chosen.isEmpty()) return null;

        List<String> tableIds = new ArrayList<>();

        // ✅ Atomic: if any update fails -> throw -> caller will rollback
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE restaurant_table
            SET status = 'OCCUPIED',
                reserved_for_reservation_id = ?,
                reserved_until = NULL
            WHERE table_id = ? AND status = 'FREE'
        """)) {
            for (TableCandidate t : chosen) {
                ps.setInt(1, reservationId);
                ps.setString(2, t.tableId);

                int updated = ps.executeUpdate();
                if (updated != 1) {
                    throw new Exception("Allocation failed, table was taken: " + t.tableId);
                }
                tableIds.add(t.tableId);
            }
        }

        return tableIds;
    }

    /**
     * Reserves one or more FREE tables for a PENDING reservation.
     * <p>
     * Uses best-fit allocation and holds the tables
     * for a limited time window.
     *
     * @param conn active database connection
     * @param reservationId reservation identifier
     * @param numCustomers party size
     * @param holdMinutes reservation hold duration
     * @return list of reserved table ids
     * @throws Exception if reservation fails (atomic operation)
     */

    public static List<String> reserveFreeTablesBestFitForReservation(Connection conn, int reservationId, int numCustomers, int holdMinutes) throws Exception {
        // 1) try existing single-table reserve logic if possible
        String one = reserveFreeTableForReservation(conn, numCustomers, reservationId, holdMinutes);
        if (one != null) {
        List<String> list = new ArrayList<>();
        list.add(one);
        return list;
        }

        // 2) DP best-fit
        List<TableCandidate> free = getFreeTables(conn);
        List<TableCandidate> chosen = chooseBestFit(free, numCustomers);
        if (chosen == null || chosen.isEmpty()) return null;

        List<String> tableIds = new ArrayList<>();

        // ✅ Atomic: if any update fails -> throw -> caller will rollback
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE restaurant_table
            SET status = 'RESERVED',
                reserved_for_reservation_id = ?,
                reserved_until = DATE_ADD(NOW(), INTERVAL ? MINUTE)
            WHERE table_id = ? AND status = 'FREE'
        """)) {
            for (TableCandidate t : chosen) {
                ps.setInt(1, reservationId);
                ps.setInt(2, holdMinutes);
                ps.setString(3, t.tableId);

                int updated = ps.executeUpdate();
                if (updated != 1) {
                    throw new Exception("Reserve failed, table was taken: " + t.tableId);
                }
                tableIds.add(t.tableId);
            }
        }

        return tableIds;
    }

    public static List<String> getReservedTablesForReservation(Connection conn, int reservationId) throws Exception {
        String sql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'RESERVED'
              AND reserved_for_reservation_id = ?
              AND reserved_until > NOW()
            ORDER BY num_of_seats DESC
        """;
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        }
        return out;
    }
    /**
     * Converts all RESERVED tables of a reservation into OCCUPIED.
     * Used when a customer checks in.
     *
     * @param conn active database connection
     * @param reservationId reservation identifier
     * @return number of tables updated
     * @throws Exception on database error
     */
    public static int occupyReservedTablesForReservation(Connection conn, int reservationId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE restaurant_table
            SET status = 'OCCUPIED',
                reserved_until = NULL
            WHERE reserved_for_reservation_id = ?
              AND status = 'RESERVED'
        """)) {
            ps.setInt(1, reservationId);
            return ps.executeUpdate();
        }
    }
    /**
     * Frees all OCCUPIED tables linked to a reservation.
     * Used after payment / checkout.
     *
     * @param conn active database connection
     * @param reservationId reservation identifier
     * @return number of tables released
     * @throws Exception on database error
     */
    public static int freeOccupiedTablesForReservation(Connection conn, int reservationId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE restaurant_table
            SET status = 'FREE',
                reserved_for_reservation_id = NULL,
                reserved_until = NULL
            WHERE reserved_for_reservation_id = ?
              AND status = 'OCCUPIED'
        """)) {
            ps.setInt(1, reservationId);
            return ps.executeUpdate();
        }
    }
    
 // ==============================
 // Waiting List - Option B (multi-table)
 // ==============================

 /**
  * Reserve MULTIPLE tables for a waiting-list entry (hold).
  * Marks them RESERVED, sets reserved_for_waiting_id and reserved_until.
  */
 public static List<String> reserveFreeTablesBestFitForWaiting(Connection conn, int waitingId, int people, int holdMinutes) throws Exception {

     // 1) try your existing single-table waiting reserve first
     String one = reserveFreeTableForWaitingReturnTableId(conn, waitingId, people);
     if (one != null){
    	 List<String> list = new ArrayList<>();
    	 list.add(one);
    	 return list; 
    	 }

     // 2) DP best-fit on all FREE tables
     List<TableCandidate> free = getFreeTables(conn);
     List<TableCandidate> chosen = chooseBestFit(free, people);
     if (chosen == null || chosen.isEmpty()) return null;

     List<String> tableIds = new ArrayList<>();

     // IMPORTANT: atomic behavior - throw if any table was taken so caller can rollback
     try (PreparedStatement ps = conn.prepareStatement("""
         UPDATE restaurant_table
         SET status = 'RESERVED',
             reserved_for_waiting_id = ?,
             reserved_until = DATE_ADD(NOW(), INTERVAL ? MINUTE)
         WHERE table_id = ?
           AND status = 'FREE'
           AND reserved_for_reservation_id IS NULL
           AND reserved_for_waiting_id IS NULL
     """)) {
         for (TableCandidate t : chosen) {
             ps.setInt(1, waitingId);
             ps.setInt(2, holdMinutes);
             ps.setString(3, t.tableId);

             int updated = ps.executeUpdate();
             if (updated != 1) {
                 throw new Exception("Waiting reserve failed, table was taken: " + t.tableId);
             }
             tableIds.add(t.tableId);
         }
     }

     return tableIds;
 }

 /**
  * Get ALL reserved tables for waitingId (still within reserved_until window).
  */
 public static List<String> getReservedTablesForWaiting(Connection conn, int waitingId) throws Exception {
     String sql = """
         SELECT table_id
         FROM restaurant_table
         WHERE status = 'RESERVED'
           AND reserved_for_waiting_id = ?
           AND reserved_until IS NOT NULL
           AND reserved_until >= NOW()
         ORDER BY num_of_seats DESC
     """;

     List<String> out = new ArrayList<>();
     try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, waitingId);
         try (ResultSet rs = ps.executeQuery()) {
             while (rs.next()) out.add(rs.getString("table_id"));
         }
     }
     return out;
 }

 /**
  * Occupy ALL reserved tables for waitingId (customer arrived / checked-in).
  * NOTE: we KEEP reserved_for_waiting_id so we can free all tables on pay.
  */
 public static int occupyReservedTablesForWaiting(Connection conn, int waitingId) throws Exception {
     String sql = """
         UPDATE restaurant_table
         SET status = 'OCCUPIED',
             reserved_until = NULL
         WHERE status = 'RESERVED'
           AND reserved_for_waiting_id = ?
           AND reserved_until IS NOT NULL
           AND reserved_until >= NOW()
     """;

     try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, waitingId);
         return ps.executeUpdate();
     }
 }

 /**
  * Free ALL occupied tables for waitingId (after payment).
  */
 public static int freeOccupiedTablesForWaiting(Connection conn, int waitingId) throws Exception {
     String sql = """
         UPDATE restaurant_table
         SET status = 'FREE',
             reserved_for_waiting_id = NULL,
             reserved_until = NULL
         WHERE status = 'OCCUPIED'
           AND reserved_for_waiting_id = ?
     """;

     try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, waitingId);
         return ps.executeUpdate();
     }
 }
 
 /**
  * Calculates the total number of seats currently available in the restaurant.
  * <p>
  * This method queries the sum of seats for all tables that currently have the status 'FREE'.
  * It uses the provided connection to execute the query.
  * </p>
  *
  * @param conn the active database connection
  * @return the total count of free seats
  * @throws Exception if a database access error occurs
  */
 public static int getTotalSeatsAvailable(Connection conn) throws Exception {
	    String sql = """
	        SELECT COALESCE(SUM(num_of_seats),0) AS total
	        FROM restaurant_table
	        WHERE status = 'FREE'
	    """;

	    try (PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {
	        return rs.next() ? rs.getInt("total") : 0;
	    }
	}

 /**
  * Calculates the total seat capacity for a specific list of table IDs.
  * <p>
  * This helper method dynamically builds a {@code WHERE table_id IN (...)} query
  * to sum the {@code num_of_seats} for the provided tables.
  * Returns 0 if the list is null or empty.
  * </p>
  *
  * @param conn the active database connection
  * @param tableIds a list of table IDs to sum
  * @return the combined seat count of the specified tables
  * @throws Exception if a database access error occurs
  */
	public static int sumSeatsForTables(Connection conn, List<String> tableIds) throws Exception {
	    if (tableIds == null || tableIds.isEmpty()) return 0;

	    // Build: IN (?,?,?,...)
	    String placeholders = String.join(",", Collections.nCopies(tableIds.size(), "?"));

	    String sql = """
	        SELECT COALESCE(SUM(num_of_seats),0) AS total
	        FROM restaurant_table
	        WHERE table_id IN (""" + placeholders + ")";

	    try (PreparedStatement ps = conn.prepareStatement(sql)) {
	        for (int i = 0; i < tableIds.size(); i++) {
	            ps.setString(i + 1, tableIds.get(i));
	        }
	        try (ResultSet rs = ps.executeQuery()) {
	            return rs.next() ? rs.getInt("total") : 0;
	        }
	    }
	}
	
	/**
	 * Calculates the total number of seats currently held for pending reservations.
	 * <p>
	 * This method sums the seats of tables that are marked 'RESERVED' and are linked
	 * to a reservation with status 'PENDING'. This represents capacity that is
	 * technically "taken" but not yet occupied by customers (they haven't arrived yet).
	 * </p>
	 *
	 * @param conn the active database connection
	 * @return the total number of reserved seats for pending reservations
	 * @throws Exception if a database access error occurs
	 */
	public static int sumReservedSeatsForPendingReservations(Connection conn) throws Exception {
	    String sql = """
	        SELECT COALESCE(SUM(t.num_of_seats), 0) AS total
	        FROM restaurant_table t
	        JOIN reservation r ON r.reservation_id = t.reserved_for_reservation_id
	        WHERE t.status = 'RESERVED'
	          AND t.reserved_until IS NOT NULL
	          AND t.reserved_until >= NOW()
	          AND r.status = 'PENDING'
	    """;

	    try (PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {
	        return rs.next() ? rs.getInt("total") : 0;
	    }
	}
	
	/**
	 * Retrieves a complete list of all restaurant tables from the database.
	 * <p>
	 * Returns a list of {@link RestaurantTableDTO} objects, including their ID,
	 * seat count, and current status (FREE, OCCUPIED, RESERVED).
	 * The list is sorted by {@code table_id}.
	 * </p>
	 *
	 * @return a list of all restaurant tables
	 * @throws Exception if a database access error occurs
	 */
	public static List<RestaurantTableDTO> getAllTables() throws Exception {
        List<RestaurantTableDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM restaurant_table ORDER BY table_id";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new RestaurantTableDTO(
                    rs.getString("table_id"),
                    rs.getInt("num_of_seats"),
                    rs.getString("status")
                ));
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }
	
	/**
	 * Permanently deletes a table from the database by its ID.
	 * <p>
	 * <b>Note:</b> This method performs the raw SQL deletion. It does not check
	 * if the table is currently occupied or needed for future reservations.
	 * The {@link #isSafeToDelete(String)} method should usually be called before this.
	 * </p>
	 *
	 * @param tableId the ID of the table to delete
	 * @return {@code true} if the table was found and deleted; {@code false} otherwise
	 * @throws Exception if a database access error occurs
	 */
	public static boolean deleteTable(String tableId) throws Exception {
        String sql = "DELETE FROM restaurant_table WHERE table_id = ?";
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            return ps.executeUpdate() > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }
	
	/**
	 * Updates the seating capacity of an existing table.
	 * <p>
	 * Modifies the {@code num_of_seats} column for the specified table ID.
	 * </p>
	 *
	 * @param tableId the ID of the table to update
	 * @param newSeats the new number of seats
	 * @return {@code true} if the table was found and updated; {@code false} otherwise
	 * @throws Exception if a database access error occurs
	 */
	public static boolean updateTableSeats(String tableId, int newSeats) throws Exception {
        String sql = "UPDATE restaurant_table SET num_of_seats = ? WHERE table_id = ?";
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newSeats);
            ps.setString(2, tableId);
            return ps.executeUpdate() > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }

	// ====== Policy 1: Table-feasibility planning (NO status changes) ======

	/**
	 * Load ALL restaurant tables (capacity list), regardless of status.
	 * Planning assumes the table inventory is the "container set".
	 */
	private static List<TableCandidate> getAllTablesForPlanning(Connection conn) throws Exception {
	    String sql = """
	        SELECT table_id, num_of_seats
	        FROM restaurant_table
	        ORDER BY num_of_seats DESC
	    """;
	    List<TableCandidate> out = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {
	        while (rs.next()) {
	            out.add(new TableCandidate(rs.getString("table_id"), rs.getInt("num_of_seats")));
	        }
	    }
	    return out;
	}

	/**
	 * Load overlapping reservation party sizes for [start, end) window.
	 * Only statuses that truly occupy seating capacity are included.
	 */
	private static List<Integer> getOverlappingConfirmedArrivedPartySizes(Connection conn,
	                                                                     java.sql.Timestamp start,
	                                                                     java.sql.Timestamp end) throws Exception {
	    String sql = """
	        SELECT num_of_customers
	        FROM reservation
	        WHERE status IN ('CONFIRMED','ARRIVED')
	          AND reservation_time < ?
	          AND expiry_time > ?
	        ORDER BY num_of_customers DESC
	    """;

	    List<Integer> parties = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement(sql)) {
	        ps.setTimestamp(1, end);
	        ps.setTimestamp(2, start);
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                int p = rs.getInt(1);
	                if (p > 0) parties.add(p);
	            }
	        }
	    }
	    return parties;
	}

	/**
	 * POLICY 1 core:
	 * Can we pack ALL overlapping reservations + newParty into the table inventory,
	 * allowing MULTI-table per reservation, but NOT sharing a table between reservations?
	 *
	 * This does NOT touch restaurant_table.status (planning only).
	 */
	public static boolean canPackReservationsAtTime(Connection conn,
	                                                java.sql.Timestamp start,
	                                                java.sql.Timestamp end,
	                                                int newParty) throws Exception {
	    if (newParty <= 0) return false;

	    // 1) load table inventory
	    List<TableCandidate> remainingTables = getAllTablesForPlanning(conn);
	    if (remainingTables.isEmpty()) return false;

	    // 2) load overlapping parties (CONFIRMED + ARRIVED)
	    List<Integer> parties = getOverlappingConfirmedArrivedPartySizes(conn, start, end);
	    parties.add(newParty);

	    // 3) pack biggest parties first (greedy order improves success)
	    parties.sort(Collections.reverseOrder());

	    // 4) for each party, choose subset of remaining tables via your DP best-fit
	    for (int partySize : parties) {
	        List<TableCandidate> chosen = chooseBestFit(remainingTables, partySize);
	        if (chosen == null || chosen.isEmpty()) {
	            return false; // cannot seat this party with remaining tables
	        }

	        // remove chosen tables from remaining (no reuse allowed)
	        // use table_id matching for safe removal
	        for (TableCandidate c : chosen) {
	            remainingTables.removeIf(t -> t.tableId.equals(c.tableId));
	        }
	    }

	    return true;
	}

	/**
	 * Convenience overload that opens its own connection.
	 */
	public static boolean canPackReservationsAtTime(java.sql.Timestamp start,
	                                                java.sql.Timestamp end,
	                                                int newParty) throws Exception {
	    MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
	    PooledConnection pc = pool.getConnection();
	    Connection conn = pc.getConnection();
	    try {
	        return canPackReservationsAtTime(conn, start, end, newParty);
	    } finally {
	        pool.releaseConnection(pc);
	    }
	}

	/**
	 * Simulates whether the current inventory of FREE tables can accommodate upcoming reservations.
	 * <p>
	 * This method performs a "dry run" allocation planning logic:
	 * </p>
	 * <ol>
	 * <li>Snapshots all currently FREE tables.</li>
	 * <li>Fetches the party sizes of upcoming reservations (within the look-ahead window).</li>
	 * <li>Iteratively attempts to match each reservation to the best-fit subset of free tables.</li>
	 * </ol>
	 * If the simulation successfully finds a seat configuration for every upcoming reservation
	 * without running out of tables, it returns {@code true}.
	 * This is useful for "Safe Delete" checks or capacity planning warnings.
	 * 
	 *
	 * @param conn the active database connection
	 * @param lookAheadMinutes how many minutes into the future to scan for upcoming reservations
	 * @param limitReservations maximum number of upcoming reservations to consider (for performance)
	 * @return {@code true} if all upcoming reservations can be seated with the current free tables; {@code false} otherwise.
	 * @throws Exception if a database access error occurs
	 */
	public static boolean canSeatUpcomingReservationsFromFreeTables(
	        Connection conn,
	        int lookAheadMinutes,
	        int limitReservations
	) throws Exception {

	    // 1) snapshot FREE tables (table_id + num_of_seats)
	    List<TableCandidate> free = getFreeTablesForPlanning(conn); // you add this helper
	    if (free == null || free.isEmpty()) return false;

	    // 2) upcoming reservations sizes (ordered by time)
	    List<Integer> needs = ReservationDAO.getUpcomingReservationSizes(conn, lookAheadMinutes, limitReservations);

	    // 3) simulate packing using your DP
	    for (int people : needs) {
	        List<TableCandidate> chosen = chooseBestFit(free, people);
	        if (chosen == null) return false;
	        free.removeAll(chosen);
	    }
	    return true;
	}

	/**
	 * Retrieves a snapshot of all currently FREE tables for planning purposes.
	 * <p>
	 * Returns a list of {@link TableCandidate} objects containing the ID and seat count
	 * for every table with status 'FREE', sorted by seat count in ascending order.
	 * This list is typically used by simulation algorithms (like {@link #canSeatUpcomingReservationsFromFreeTables})
	 * to test allocation strategies without modifying the actual database.
	 * </p>
	 *
	 * @param conn the active database connection
	 * @return a list of table candidates representing the current free inventory
	 * @throws Exception if a database access error occurs
	 */
	private static List<TableCandidate> getFreeTablesForPlanning(Connection conn) throws Exception {
	    List<TableCandidate> list = new ArrayList<>();
	    try (PreparedStatement ps = conn.prepareStatement("""
	        SELECT table_id, num_of_seats
	        FROM restaurant_table
	        WHERE status = 'FREE'
	        ORDER BY num_of_seats ASC
	    """);
	         ResultSet rs = ps.executeQuery()) {
	        while (rs.next()) {
	            list.add(new TableCandidate(rs.getString("table_id"), rs.getInt("num_of_seats")));
	        }
	    }
	    return list;
	}
	
	/**
	 * Checks if a table is safe to be deleted from the system.
	 * <p>
	 * Performs three distinct checks:
	 * 1. <b>Status Check:</b> Ensures the table is currently "FREE" (not OCCUPIED or RESERVED).
	 * 2. <b>Active Visit Check:</b> Ensures no customers are currently seated (linked via the visit table).
	 * 3. <b>Future Capacity Check:</b> Ensures that removing this table will not cause overbooking
	 * for any future reservation slot where the total booked guests would exceed the new total capacity.
	 * </p>
	 *
	 * @param tableId the ID of the table to check
	 * @return {@code null} if safe to delete; otherwise, a {@code String} containing the error message explaining the conflict.
	 * @throws Exception if a database access error occurs
	 */
    public static String isSafeToDelete(String tableId) throws Exception {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            // 1. Check Current Status & Seat Count
            String sqlStatus = "SELECT status, num_of_seats FROM restaurant_table WHERE table_id = ?";
            int tableSeats = 0;
            
            try (PreparedStatement ps = conn.prepareStatement(sqlStatus)) {
                ps.setString(1, tableId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        tableSeats = rs.getInt("num_of_seats");
                        
                        if (!"FREE".equalsIgnoreCase(status)) {
                            return "Table is currently " + status + ".";
                        }
                    } else {
                        return "Table not found.";
                    }
                }
            }

            // 2. Check Active Visits (Double safety: checking the visit log)
            String sqlVisit = "SELECT COUNT(*) FROM visit WHERE table_id = ? AND actual_end_time IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sqlVisit)) {
                ps.setString(1, tableId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "Table is currently occupied by active diners.";
                    }
                }
            }

            // 3. Check Future Capacity Conflicts
            // Calculate what the NEW capacity would be if we delete this table
            int currentTotalCapacity = getTotalSeats(); // Uses existing method
            int newTotalCapacity = currentTotalCapacity - tableSeats;

            // Find any future time slot where Booked Guests > New Capacity
            // Note: This assumes aligned reservation slots. For strict overlap checks, logic is more complex.
            String sqlCapacity = """
                SELECT reservation_time, SUM(num_of_customers) as booked_count
                FROM reservation
                WHERE status IN ('CONFIRMED') AND reservation_time > NOW()
                GROUP BY reservation_time
                HAVING booked_count > ?
                LIMIT 1
            """;

            try (PreparedStatement ps = conn.prepareStatement(sqlCapacity)) {
                ps.setInt(1, newTotalCapacity);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        java.sql.Timestamp conflictTime = rs.getTimestamp("reservation_time");
                        int booked = rs.getInt("booked_count");
                        return "Cannot delete: Overbooking conflict on " + conflictTime + 
                               " (Booked: " + booked + ", Capacity after delete: " + newTotalCapacity + ")";
                    }
                }
            }

            return null; // Safe to delete (No errors found)

        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    /**
     * Inserts a new table into the database with an automatically generated ID.
     * <p>
     * This method calculates the next available ID in the sequence (e.g., T10 -> T11)
     * by finding the current maximum numeric suffix in the 'restaurant_table' table.
     * It is thread-safe regarding the connection pool but does not lock the table for strict serialization.
     * </p>
     *
     * @param seats the number of seats for the new table
     * @return the generated table ID (e.g., "T11")
     * @throws Exception if a database access error occurs or generation fails
     */
    public static String insertTableAutoId(int seats) throws Exception {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            // 1. Find the highest existing ID (Assuming format 'T' + number, e.g., T1, T2, T10)
            // We cast the substring to UNSIGNED to ensure T10 comes after T2
            String sqlMax = """
                SELECT table_id 
                FROM restaurant_table 
                WHERE table_id LIKE 'T%' 
                ORDER BY CAST(SUBSTRING(table_id, 2) AS UNSIGNED) DESC 
                LIMIT 1
            """;
            
            String nextId = "T1"; // Default if empty

            try (PreparedStatement ps = conn.prepareStatement(sqlMax);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String currentMax = rs.getString(1);
                    try {
                        // Extract number part (e.g. "10" from "T10")
                        int num = Integer.parseInt(currentMax.substring(1));
                        nextId = "T" + (num + 1);
                    } catch (NumberFormatException e) {
                        // Fallback if ID format is weird, just append timestamp or random
                        nextId = "T" + (System.currentTimeMillis() % 10000);
                    }
                }
            }

            // 2. Insert the new table
            String sqlInsert = "INSERT INTO restaurant_table (table_id, num_of_seats, status) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setString(1, nextId);
                ps.setInt(2, seats);
                ps.setString(3, "FREE");
                ps.executeUpdate();
            }
            
            return nextId;

        } finally {
            pool.releaseConnection(pc);
        }
    }


}