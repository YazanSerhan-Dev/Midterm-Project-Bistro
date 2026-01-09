package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class RestaurantTableDAO {

    public static void insertTable(
            String tableId,
            int seats,
            String status) throws Exception {

        String sql = """
            INSERT INTO restaurant_table
            (table_id, num_of_seats, status)
            VALUES (?, ?, ?)
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
    
    public static int freeTablesForExpiredReservations() throws Exception {

        String sql = """
            UPDATE restaurant_table t
            JOIN visit v ON v.table_id = t.table_id
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            JOIN reservation r ON r.reservation_id = ua.reservation_id
            SET t.status = 'FREE'
            WHERE r.status = 'EXPIRED'
              AND t.status = 'OCCUPIED'
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

    public static int getMaxTableSeats() throws Exception {
        String sql = "SELECT COALESCE(MAX(num_of_seats),0) AS maxSeats FROM restaurant_table";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt("maxSeats");
            return 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
 // === Option-1: REAL reservation hold using restaurant_table columns ===

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

    public static boolean occupyReservedTableForReservation(Connection conn, String tableId, int reservationId) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'OCCUPIED',
                reserved_for_reservation_id = NULL,
                reserved_until = NULL
            WHERE table_id = ?
              AND status = 'RESERVED'
              AND reserved_for_reservation_id = ?
              AND reserved_until IS NOT NULL
              AND reserved_until > NOW()
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            ps.setInt(2, reservationId);
            return ps.executeUpdate() == 1;
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

    public static String getReservedTableForWaiting(Connection conn, int waitingId) throws Exception {
        String sql = """
            SELECT table_id
            FROM restaurant_table
            WHERE status = 'RESERVED'
              AND reserved_for_waiting_id = ?
              AND reserved_until IS NOT NULL
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("table_id");
                return null;
            }
        }
    }

    public static boolean occupyReservedTableForWaiting(Connection conn, String tableId, int waitingId) throws Exception {
        String sql = """
            UPDATE restaurant_table
            SET status = 'OCCUPIED',
                reserved_for_waiting_id = NULL,
                reserved_until = NULL
            WHERE table_id = ?
              AND status = 'RESERVED'
              AND reserved_for_waiting_id = ?
              AND reserved_until IS NOT NULL
              AND reserved_until >= NOW()
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            ps.setInt(2, waitingId);
            return ps.executeUpdate() == 1;
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
     * Allocate MULTIPLE tables for a CONFIRMED reservation at check-in time.
     * Marks them OCCUPIED and sets reserved_for_reservation_id = reservationId
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
            return List.of(single);
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
     * Reserve MULTIPLE tables for a PENDING reservation (hold).
     * Marks them RESERVED, sets reserved_for_reservation_id and reserved_until.
     */
    public static List<String> reserveFreeTablesBestFitForReservation(Connection conn, int reservationId, int numCustomers, int holdMinutes) throws Exception {
        // 1) try existing single-table reserve logic if possible
        String one = reserveFreeTableForReservation(conn, numCustomers, reservationId, holdMinutes);
        if (one != null) return List.of(one);

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
     * Convert all RESERVED tables (for reservation) into OCCUPIED on check-in.
     * Keep reserved_for_reservation_id so we can free all after payment.
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
     if (one != null) return List.of(one);

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



}
