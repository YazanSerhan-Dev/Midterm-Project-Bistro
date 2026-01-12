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

public class RestaurantTableDAO {

    // =============================================================
    // 1. BASIC INSERT (Common)
    // =============================================================
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

    // =============================================================
    // 2. MANAGEMENT METHODS (From HEAD/Your Branch)
    // =============================================================
    
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

    // =============================================================
    // 3. SEAT COUNTING LOGIC (Common / Mixed)
    // =============================================================

    public static int getTotalSeatsAvailable() throws Exception {
        String sql = "SELECT COALESCE(SUM(num_of_seats),0) AS total FROM restaurant_table WHERE status = 'FREE'";
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
    
    public static int getTotalSeats() throws Exception {
        String sql = "SELECT COALESCE(SUM(num_of_seats),0) AS total FROM restaurant_table";
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

    // =============================================================
    // 4. COMPLEX ALLOCATION LOGIC (From MAIN Branch)
    // =============================================================

    private static class TableCandidate {
        final String tableId;
        final int seats;
        TableCandidate(String tableId, int seats) {
            this.tableId = tableId;
            this.seats = seats;
        }
    }

    private static List<TableCandidate> getFreeTables(Connection conn) throws Exception {
        String sql = "SELECT table_id, num_of_seats FROM restaurant_table WHERE status = 'FREE' ORDER BY num_of_seats DESC";
        List<TableCandidate> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new TableCandidate(rs.getString(1), rs.getInt(2)));
            }
        }
        return out;
    }

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
                if (!dp[ns] || newCount < count[ns]) {
                    dp[ns] = true;
                    count[ns] = newCount;
                    prevSum[ns] = s;
                    prevIdx[ns] = i;
                }
            }
        }

        int bestSum = -1;
        for (int s = needed; s <= maxSum; s++) {
            if (!dp[s]) continue;
            if (bestSum == -1) { bestSum = s; continue; }
            int waste = s - needed;
            int bestWaste = bestSum - needed;
            if (waste < bestWaste) bestSum = s;
            else if (waste == bestWaste && count[s] < count[bestSum]) bestSum = s;
        }

        if (bestSum == -1) return null;

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

    // --- Single Table Logic ---

    public static String allocateFreeTable(Connection conn, int numCustomers) throws Exception {
        String selectSql = "SELECT table_id FROM restaurant_table WHERE status = 'FREE' AND num_of_seats >= ? ORDER BY num_of_seats ASC LIMIT 1";
        String updateSql = "UPDATE restaurant_table SET status = 'OCCUPIED' WHERE table_id = ? AND status = 'FREE'";
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
            if (ps2.executeUpdate() != 1) return null;
        }
        return tableId;
    }
    
    public static String reserveFreeTableForReservation(Connection conn, int numCustomers, int reservationId, int holdMinutes) throws Exception {
        String selectSql = "SELECT table_id FROM restaurant_table WHERE status = 'FREE' AND num_of_seats >= ? ORDER BY num_of_seats ASC LIMIT 1";
        String updateSql = "UPDATE restaurant_table SET status = 'RESERVED', reserved_for_reservation_id = ?, reserved_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE table_id = ? AND status = 'FREE'";
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
            if (ps.executeUpdate() == 0) return null;
        }
        return tableId;
    }
    
    // --- Multi-Table Logic ---

    public static List<String> allocateFreeTablesBestFit(Connection conn, int reservationId, int numCustomers) throws Exception {
        String single = allocateFreeTable(conn, numCustomers);
        if (single != null) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_table SET reserved_for_reservation_id = ? WHERE table_id = ?")) {
                ps.setInt(1, reservationId);
                ps.setString(2, single);
                ps.executeUpdate();
            }
            return List.of(single);
        }
        List<TableCandidate> free = getFreeTables(conn);
        List<TableCandidate> chosen = chooseBestFit(free, numCustomers);
        if (chosen == null || chosen.isEmpty()) return null;
        List<String> tableIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_table SET status = 'OCCUPIED', reserved_for_reservation_id = ?, reserved_until = NULL WHERE table_id = ? AND status = 'FREE'")) {
            for (TableCandidate t : chosen) {
                ps.setInt(1, reservationId);
                ps.setString(2, t.tableId);
                if (ps.executeUpdate() != 1) throw new Exception("Allocation failed: " + t.tableId);
                tableIds.add(t.tableId);
            }
        }
        return tableIds;
    }

    public static List<String> reserveFreeTablesBestFitForReservation(Connection conn, int reservationId, int numCustomers, int holdMinutes) throws Exception {
        String one = reserveFreeTableForReservation(conn, numCustomers, reservationId, holdMinutes);
        if (one != null) return List.of(one);
        List<TableCandidate> free = getFreeTables(conn);
        List<TableCandidate> chosen = chooseBestFit(free, numCustomers);
        if (chosen == null || chosen.isEmpty()) return null;
        List<String> tableIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_table SET status = 'RESERVED', reserved_for_reservation_id = ?, reserved_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE table_id = ? AND status = 'FREE'")) {
            for (TableCandidate t : chosen) {
                ps.setInt(1, reservationId);
                ps.setInt(2, holdMinutes);
                ps.setString(3, t.tableId);
                if (ps.executeUpdate() != 1) throw new Exception("Reserve failed: " + t.tableId);
                tableIds.add(t.tableId);
            }
        }
        return tableIds;
    }

    // --- Cleanup & Helpers ---

    public static boolean freeTable(Connection conn, String tableId) throws Exception {
        String sql = "UPDATE restaurant_table SET status = 'FREE' WHERE table_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableId);
            return ps.executeUpdate() > 0;
        }
    }

    public static int releaseExpiredReservedTables(Connection conn) throws Exception {
        String sql = "UPDATE restaurant_table SET status = 'FREE', reserved_for_reservation_id = NULL, reserved_until = NULL WHERE status = 'RESERVED' AND reserved_until IS NOT NULL AND reserved_until <= NOW()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { return ps.executeUpdate(); }
    }
    
    public static int releaseExpiredReservedTables() throws Exception {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();
        try { return releaseExpiredReservedTables(conn); } finally { pool.releaseConnection(pc); }
    }

    public static List<String> getReservedTablesForReservation(Connection conn, int reservationId) throws Exception {
        String sql = "SELECT table_id FROM restaurant_table WHERE status = 'RESERVED' AND reserved_for_reservation_id = ? AND reserved_until > NOW() ORDER BY num_of_seats DESC";
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getString(1)); }
        }
        return out;
    }

    public static int occupyReservedTablesForReservation(Connection conn, int reservationId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_table SET status = 'OCCUPIED', reserved_until = NULL WHERE reserved_for_reservation_id = ? AND status = 'RESERVED'")) {
            ps.setInt(1, reservationId);
            return ps.executeUpdate();
        }
    }

    public static int freeOccupiedTablesForReservation(Connection conn, int reservationId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_table SET status = 'FREE', reserved_for_reservation_id = NULL, reserved_until = NULL WHERE reserved_for_reservation_id = ? AND status = 'OCCUPIED'")) {
            ps.setInt(1, reservationId);
            return ps.executeUpdate();
        }
    }

    // --- Waiting List Logic ---

    public static String reserveFreeTableForWaitingReturnTableId(Connection conn, int waitingId, int people) throws Exception {
        String select = "SELECT table_id FROM restaurant_table WHERE status = 'FREE' AND num_of_seats >= ? AND reserved_for_reservation_id IS NULL AND reserved_for_waiting_id IS NULL ORDER BY num_of_seats ASC LIMIT 1 FOR UPDATE";
        String update = "UPDATE restaurant_table SET status = 'RESERVED', reserved_for_waiting_id = ?, reserved_until = DATE_ADD(NOW(), INTERVAL 15 MINUTE) WHERE table_id = ? AND status = 'FREE'";
        String tableId = null;
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, people);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) tableId = rs.getString("table_id"); }
        }
        if (tableId == null) return null;
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, waitingId);
            ps.setString(2, tableId);
            return (ps.executeUpdate() == 1) ? tableId : null;
        }
    }

    public static List<String> reserveFreeTablesBestFitForWaiting(Connection conn, int waitingId, int people, int holdMinutes) throws Exception {
        String one = reserveFreeTableForWaitingReturnTableId(conn, waitingId, people);
        if (one != null) return List.of(one);
        List<TableCandidate> free = getFreeTables(conn);
        List<TableCandidate> chosen = chooseBestFit(free, people);
        if (chosen == null || chosen.isEmpty()) return null;
        List<String> tableIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE restaurant_table SET status = 'RESERVED', reserved_for_waiting_id = ?, reserved_until = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE table_id = ? AND status = 'FREE'")) {
            for (TableCandidate t : chosen) {
                ps.setInt(1, waitingId);
                ps.setInt(2, holdMinutes);
                ps.setString(3, t.tableId);
                if (ps.executeUpdate() != 1) throw new Exception("Waiting reserve failed: " + t.tableId);
                tableIds.add(t.tableId);
            }
        }
        return tableIds;
    }

    public static int releaseExpiredReservedTablesForWaiting() throws Exception {
        String sql = "UPDATE restaurant_table SET status = 'FREE', reserved_for_waiting_id = NULL, reserved_until = NULL WHERE status = 'RESERVED' AND reserved_for_waiting_id IS NOT NULL AND reserved_until IS NOT NULL AND reserved_until < NOW()";
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) { return ps.executeUpdate(); } finally { pool.releaseConnection(pc); }
    }

    public static int occupyReservedTablesForWaiting(Connection conn, int waitingId) throws Exception {
        String sql = "UPDATE restaurant_table SET status = 'OCCUPIED', reserved_until = NULL WHERE status = 'RESERVED' AND reserved_for_waiting_id = ? AND reserved_until IS NOT NULL AND reserved_until >= NOW()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeUpdate();
        }
    }

    public static int freeOccupiedTablesForWaiting(Connection conn, int waitingId) throws Exception {
        String sql = "UPDATE restaurant_table SET status = 'FREE', reserved_for_waiting_id = NULL, reserved_until = NULL WHERE status = 'OCCUPIED' AND reserved_for_waiting_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeUpdate();
        }
    }
}