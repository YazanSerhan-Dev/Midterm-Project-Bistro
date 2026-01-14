package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.SubscriberDTO;
import common.dto.TerminalValidateResponseDTO;
import common.dto.WaitingListDTO;

public class WaitingListDAO {

    // =============================================================
    // 1. METHODS FROM HEAD (Your Dashboard Logic)
    // =============================================================

    public static void insertWaiting(
            int numOfCustomers,
            Timestamp requestTime,
            String status,
            String confirmationCode) throws Exception {

        String sql = """
                INSERT INTO waiting_list
                (num_of_customers, request_time, status, confirmation_code)
                VALUES (?, ?, ?, ?)
            """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numOfCustomers);
            ps.setTimestamp(2, requestTime);
            ps.setString(3, status);
            ps.setString(4, confirmationCode);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static List<WaitingListDTO> getAllWaitingList() {
        List<WaitingListDTO> list = new ArrayList<>();

        String sql = "SELECT waiting_id, num_of_customers, request_time, status, confirmation_code FROM waiting_list WHERE status = 'WAITING' ORDER BY request_time ASC";

        try {
            MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
            PooledConnection pc = pool.getConnection();
            Connection conn = pc.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("waiting_id");
                int count = rs.getInt("num_of_customers");
                String status = rs.getString("status");
                String code = rs.getString("confirmation_code");
                
                java.sql.Timestamp ts = rs.getTimestamp("request_time");
                String timeStr = (ts != null) ? ts.toString() : "";

                list.add(new WaitingListDTO(id, count, timeStr, status, code));
            }

            pool.releaseConnection(pc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // =============================================================
    // 2. METHODS FROM MAIN (The Complex Logic)
    // =============================================================

    public static WaitingListDTO getByCode(String code) throws Exception {
        String sql = """
            SELECT waiting_id, num_of_customers, request_time, status, confirmation_code
            FROM waiting_list
            WHERE confirmation_code = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                WaitingListDTO dto = new WaitingListDTO();
                dto.setId(rs.getInt("waiting_id"));
                dto.setPeopleCount(rs.getInt("num_of_customers"));
                dto.setStatus(rs.getString("status"));
                dto.setConfirmationCode(rs.getString("confirmation_code"));
                return dto;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static boolean updateStatusByCode(String code, String newStatus) throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = ?
            WHERE confirmation_code = ?
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, code);
            return ps.executeUpdate() > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int cancelAssignedOver15Minutes() throws Exception {
        // 1) Cancel assigned waiting_list rows older than 15 minutes
        String cancelSql = """
            UPDATE waiting_list
            SET status = 'CANCELED'
            WHERE status = 'ASSIGNED'
              AND request_time < (NOW() - INTERVAL 15 MINUTE)
        """;

        // 2) Release tables
        String releaseTablesSql = """
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

        try {
            conn.setAutoCommit(false);
            int canceled;
            try (PreparedStatement ps = conn.prepareStatement(cancelSql)) {
                canceled = ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(releaseTablesSql)) {
                ps.executeUpdate();
            }
            conn.commit();
            return canceled;
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }

    public static String getGuestEmailForWaitingId(int waitingId) throws Exception {
        String sql = """
            SELECT guest_email
            FROM user_activity
            WHERE waiting_id = ?
            ORDER BY activity_id DESC
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static String getGuestPhoneForWaitingId(int waitingId) throws Exception {
        String sql = """
            SELECT guest_phone
            FROM user_activity
            WHERE waiting_id = ?
            ORDER BY activity_id DESC
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int insertWaitingReturnId(
            int numOfCustomers,
            Timestamp requestTime,
            String status,
            String confirmationCode) throws Exception {

        String sql = """
            INSERT INTO waiting_list
            (num_of_customers, request_time, status, confirmation_code)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, numOfCustomers);
            ps.setTimestamp(2, requestTime);
            ps.setString(3, status);
            ps.setString(4, confirmationCode);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int cancelWaitingOlderThanHours(int hours) throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = 'CANCELED'
            WHERE status = 'WAITING'
              AND request_time < (NOW() - INTERVAL ? HOUR)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hours);
            return ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    // Used inside transactions
    public static WaitingListDTO getByCode(Connection conn, String code) throws Exception {
        String sql = """
            SELECT waiting_id, num_of_customers, request_time, status, confirmation_code
            FROM waiting_list
            WHERE confirmation_code = ?
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                WaitingListDTO dto = new WaitingListDTO();
                dto.setId(rs.getInt("waiting_id"));
                dto.setPeopleCount(rs.getInt("num_of_customers"));
                dto.setStatus(rs.getString("status"));
                dto.setConfirmationCode(rs.getString("confirmation_code"));
                return dto;
            }
        }
    }

    // Used inside transactions
    public static boolean updateStatusByCode(Connection conn, String code, String newStatus) throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = ?
            WHERE confirmation_code = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, code);
            return ps.executeUpdate() > 0;
        }
    }

    public static TerminalValidateResponseDTO checkInWaitingListByCode(String code) throws Exception {

        TerminalValidateResponseDTO dto = new TerminalValidateResponseDTO();

        if (code == null || code.isBlank()) {
            dto.setValid(false);
            dto.setMessage("Invalid confirmation code.");
            return dto;
        }

        code = code.trim();

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            WaitingListDTO w = WaitingListDAO.getByCode(conn, code);
            if (w == null) {
                conn.rollback();
                dto.setValid(false);
                dto.setMessage("Code not found.");
                return dto;
            }

            String st = (w.getStatus() == null) ? "" : w.getStatus().trim();

            if (!"ASSIGNED".equalsIgnoreCase(st)) {
                conn.rollback();
                dto.setValid(true);
                dto.setStatus(st.isBlank() ? "WAITING" : st);
                dto.setNumOfCustomers(w.getPeopleCount());
                dto.setCheckInAllowed(false);
                dto.setMessage("Check-in allowed only when status is ASSIGNED.");
                dto.setTableId("-");
                return dto;
            }

            if (VisitDAO.existsVisitForWaitingId(conn, w.getId())) {
                conn.rollback();
                dto.setValid(false);
                dto.setMessage("This waiting code was already checked-in.");
                return dto;
            }

            // Multi-table support
            List<String> reservedTables = RestaurantTableDAO.getReservedTablesForWaiting(conn, w.getId());

            if (reservedTables == null || reservedTables.isEmpty()) {
                conn.rollback();
                dto.setValid(true);
                dto.setStatus("ASSIGNED");
                dto.setNumOfCustomers(w.getPeopleCount());
                dto.setCheckInAllowed(false);
                dto.setMessage("No reserved tables found (reservation may have expired). Please wait for a new assignment.");
                dto.setTableId("-");
                return dto;
            }

            int occupied = RestaurantTableDAO.occupyReservedTablesForWaiting(conn, w.getId());
            if (occupied <= 0) {
                conn.rollback();
                dto.setValid(true);
                dto.setStatus("ASSIGNED");
                dto.setNumOfCustomers(w.getPeopleCount());
                dto.setCheckInAllowed(false);
                dto.setMessage("Reserved tables expired or not available. Please wait for a new assignment.");
                dto.setTableId("-");
                return dto;
            }

            Integer activityId = null;
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT activity_id
                FROM user_activity
                WHERE waiting_id = ?
                ORDER BY activity_id DESC
                LIMIT 1
            """)) {
                ps.setInt(1, w.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) activityId = rs.getInt(1);
                }
            }

            if (activityId == null) {
                activityId = UserActivityDAO.insertWaitingActivityReturnActivityId(
                        conn, w.getId(), null, null);
            }

            if (activityId == null || activityId <= 0) {
                conn.rollback();
                dto.setValid(false);
                dto.setMessage("Failed to create activity.");
                return dto;
            }

            for (String t : reservedTables) {
                VisitDAO.insertVisit(conn, activityId, t);
            }

            WaitingListDAO.updateStatusByCode(conn, code, "ARRIVED");

            conn.commit();

            dto.setValid(true);
            dto.setStatus("ARRIVED");
            dto.setNumOfCustomers(w.getPeopleCount());
            dto.setCheckInAllowed(false);
            dto.setMessage("Checked-in successfully (waiting list).");
            dto.setTableId(reservedTables.get(0));
            return dto;

        } catch (Exception ex) {
            try { conn.rollback(); } catch (Exception ignored) {}
            dto.setValid(false);
            dto.setMessage("Server error: " + ex.getMessage());
            return dto;
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }

    public static WaitingListDTO assignNextWaitingByReservingTable() throws Exception {

        final int RESERVATION_LOOKAHEAD_MINUTES = 60;
        final int HOLD_MINUTES = 15;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            int freeSeatsNow = RestaurantTableDAO.getTotalSeatsAvailable(conn);
            if (freeSeatsNow <= 0) {
                conn.rollback();
                return null;
            }

            int pendingNeed = ReservationDAO.sumPendingSeats(conn, 20);
            int confirmedNeed = ReservationDAO.sumConfirmedDueSoonSeats(conn, RESERVATION_LOOKAHEAD_MINUTES);
            int pendingReservedSeats = RestaurantTableDAO.sumReservedSeatsForPendingReservations(conn);

            int effectivePendingNeed = Math.max(0, pendingNeed - pendingReservedSeats);
            int protectedNeed = effectivePendingNeed + confirmedNeed;

            String pick = """
                SELECT waiting_id, num_of_customers, confirmation_code
                FROM waiting_list
                WHERE status = 'WAITING'
                ORDER BY request_time ASC
                LIMIT 20
                FOR UPDATE
            """;

            try (PreparedStatement ps = conn.prepareStatement(pick);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int waitingId = rs.getInt("waiting_id");
                    int people = rs.getInt("num_of_customers");
                    String code = rs.getString("confirmation_code");

                    List<String> existing = RestaurantTableDAO.getReservedTablesForWaiting(conn, waitingId);
                    if (existing != null && !existing.isEmpty()) {
                        continue;
                    }

                    Savepoint sp = conn.setSavepoint("WL_TRY_" + waitingId);

                    List<String> tableIds = RestaurantTableDAO.reserveFreeTablesBestFitForWaiting(
                            conn, waitingId, people, HOLD_MINUTES);

                    if (tableIds == null || tableIds.isEmpty()) {
                        conn.rollback(sp);
                        continue;
                    }

                    int freeAfter = RestaurantTableDAO.getTotalSeatsAvailable(conn);
                    if (freeAfter < protectedNeed) {
                        conn.rollback(sp);
                        continue;
                    }

                    int updated;
                    try (PreparedStatement up = conn.prepareStatement("""
                        UPDATE waiting_list
                        SET status = 'ASSIGNED',
                            request_time = NOW()
                        WHERE waiting_id = ?
                          AND status = 'WAITING'
                    """)) {
                        up.setInt(1, waitingId);
                        updated = up.executeUpdate();
                    }

                    if (updated != 1) {
                        conn.rollback(sp);
                        continue;
                    }

                    conn.commit();
                    return new WaitingListDTO(waitingId, null, null, null, people, "ASSIGNED", code);
                }
            }

            conn.rollback();
            return null;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;

        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }

    public static boolean cancelAndReleaseTablesByCode(String code) throws Exception {

        if (code == null || code.isBlank()) return false;
        code = code.trim();

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            WaitingListDTO w;
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT waiting_id, status
                FROM waiting_list
                WHERE confirmation_code = ?
                LIMIT 1
                FOR UPDATE
            """)) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    w = new WaitingListDTO();
                    w.setId(rs.getInt("waiting_id"));
                    w.setStatus(rs.getString("status"));
                }
            }

            String st = (w.getStatus() == null) ? "" : w.getStatus().trim().toUpperCase();

            if ("ARRIVED".equals(st)) {
                conn.rollback();
                return false;
            }

            int updated;
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE waiting_list
                SET status = 'CANCELED'
                WHERE waiting_id = ?
                  AND status IN ('WAITING', 'ASSIGNED')
            """)) {
                ps.setInt(1, w.getId());
                updated = ps.executeUpdate();
            }

            if (updated != 1) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE restaurant_table
                SET status = 'FREE',
                    reserved_for_waiting_id = NULL,
                    reserved_until = NULL
                WHERE status = 'RESERVED'
                  AND reserved_for_waiting_id = ?
            """)) {
                ps.setInt(1, w.getId());
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;

        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }
    
 // =============================================================
    // 3. STAFF REMOVAL METHODS (Fixes "Remove / Cancel" button)
    // =============================================================

    public static boolean cancelWaitingById(int waitingId) throws Exception {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            // 1. Lock and Check Status
            String status = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM waiting_list WHERE waiting_id = ? FOR UPDATE")) {
                ps.setInt(1, waitingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        status = rs.getString("status");
                    } else {
                        conn.rollback();
                        return false; // ID not found
                    }
                }
            }

            // 2. If already arrived or canceled, do not remove again
            if ("ARRIVED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
                conn.rollback();
                return false;
            }

            // 3. Update Status to CANCELED
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE waiting_list SET status = 'CANCELED' WHERE waiting_id = ?")) {
                ps.setInt(1, waitingId);
                int rows = ps.executeUpdate();
                if (rows <= 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 4. Release any tables reserved for this waiting ID
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE restaurant_table
                SET status = 'FREE',
                    reserved_for_waiting_id = NULL,
                    reserved_until = NULL
                WHERE reserved_for_waiting_id = ?
            """)) {
                ps.setInt(1, waitingId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }
}