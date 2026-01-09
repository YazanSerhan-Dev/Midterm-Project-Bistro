package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.TerminalValidateResponseDTO;
import common.dto.WaitingListDTO;

public class WaitingListDAO {

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

    // ✅ NEW: fetch waiting list entry by its code
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

                // note: DB table doesn’t store email/phone per your schema, so we don’t fill them here.
                return dto;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // ✅ NEW: update waiting status by code (WAITING / ASSIGNED / CANCELED)
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
    
    /*
    public static common.dto.WaitingListDTO getOldestWaitingThatFits() throws Exception {
    	String sql = """
    		    SELECT *
    		    FROM waiting_list w
    		    WHERE w.status = 'WAITING'
    		      AND w.request_time <= NOW()
    		      AND EXISTS (
    		          SELECT 1
    		          FROM restaurant_table t
    		          WHERE t.status = 'FREE'
    		            AND t.num_of_seats >= w.num_of_customers
    		      )
    		      AND w.num_of_customers <= (
    		          (SELECT COALESCE(SUM(t2.num_of_seats),0)
    		           FROM restaurant_table t2
    		           WHERE t2.status = 'FREE')
    		          -
    		          (SELECT COALESCE(SUM(r.num_of_customers),0)
    		           FROM reservation r
    		           WHERE r.status = 'CONFIRMED'
    		             AND NOW() >= r.reservation_time
    		             AND NOW() <= DATE_ADD(r.reservation_time, INTERVAL 15 MINUTE))
    		      )
    		    ORDER BY w.request_time ASC
    		    LIMIT 1
    		""";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return null;

            common.dto.WaitingListDTO dto = new common.dto.WaitingListDTO();
            dto.setId(rs.getInt("waiting_id"));
            dto.setPeopleCount(rs.getInt("num_of_customers"));
            dto.setStatus(rs.getString("status"));
            dto.setConfirmationCode(rs.getString("confirmation_code"));
            return dto;

        } finally {
            pool.releaseConnection(pc);
        }
    }
    */
    
    public static boolean markAssignedById(int waitingId) throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = 'ASSIGNED',
                request_time = NOW()
            WHERE waiting_id = ?
              AND status = 'WAITING'
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeUpdate() == 1;
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

        // 2) Release tables that were reserved for waiting_list and their reserved_until already passed
        // (This is the REAL "no-show" cleanup)
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

            // Release any expired reserved tables (even if canceled=0, still good to clean)
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

    public static boolean cancelIfWaitingByCode(String code) {

        String sql = """
            UPDATE waiting_list
            SET status = 'CANCELED'
            WHERE confirmation_code = ?
              AND status = 'WAITING'
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            return ps.executeUpdate() == 1;   // true only if 1 row updated
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
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

    public static boolean cancelIfWaitingOrAssignedByCode(String code) {
        String sql = """
            UPDATE waiting_list
            SET status = 'CANCELED'
            WHERE confirmation_code = ?
              AND status IN ('WAITING', 'ASSIGNED')
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            pool.releaseConnection(pc);
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

            // only ASSIGNED can check-in
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

            // If already checked-in, block
            if (VisitDAO.existsVisitForWaitingId(conn, w.getId())) {
                conn.rollback();
                dto.setValid(false);
                dto.setMessage("This waiting code was already checked-in.");
                return dto;
            }

            // ===============================
            // ✅ OPTION B: MULTI-TABLE SUPPORT
            // ===============================

            // Get ALL reserved tables for this waiting id
            java.util.List<String> reservedTables =
                    RestaurantTableDAO.getReservedTablesForWaiting(conn, w.getId());

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

            // Occupy ALL reserved tables (atomic)
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

            // Get existing activity or create if missing
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

            // ✅ INSERT VISITS FOR *ALL* TABLES
            for (String t : reservedTables) {
                VisitDAO.insertVisit(conn, activityId, t);
            }

            // Waiting list: ASSIGNED -> ARRIVED
            WaitingListDAO.updateStatusByCode(conn, code, "ARRIVED");

            conn.commit();

            dto.setValid(true);
            dto.setStatus("ARRIVED");
            dto.setNumOfCustomers(w.getPeopleCount());
            dto.setCheckInAllowed(false);
            dto.setMessage("Checked-in successfully (waiting list).");

            // UI compatibility – show first table only
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

            // =========================================================
            // ✅ Capacity-aware reservation priority (real-life rule):
            // Allow waiting-list ONLY if we can still keep enough FREE seats
            // to satisfy PENDING reservations + CONFIRMED due soon reservations.
            //
            // Example:
            //   freeSeats=30, pendingNeed=15, waiting=6 -> 30-6 >= 15 => allow.
            // =========================================================

            int freeSeatsNow = RestaurantTableDAO.getTotalSeatsAvailable();   // FREE seats only
            int pendingNeed = ReservationDAO.sumPendingSeats(conn, 20);       // top 20 oldest pending
            int confirmedNeed = ReservationDAO.sumConfirmedDueSoonSeats(conn, RESERVATION_LOOKAHEAD_MINUTES);

            int protectedNeed = pendingNeed + confirmedNeed;

            // If there are 0 free seats, no point assigning.
            if (freeSeatsNow <= 0) {
                conn.rollback();
                return null;
            }

            String pick = """
                SELECT waiting_id, num_of_customers, status, confirmation_code
                FROM waiting_list
                WHERE status = 'WAITING'
                ORDER BY request_time ASC
                LIMIT 20
                FOR UPDATE
            """;

            try (PreparedStatement ps = conn.prepareStatement(pick);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("waiting_id");
                    int people = rs.getInt("num_of_customers");
                    String code = rs.getString("confirmation_code");

                    // ✅ If already reserved somehow, skip (safety)
                    List<String> existing = RestaurantTableDAO.getReservedTablesForWaiting(conn, id);
                    if (existing != null && !existing.isEmpty()) {
                        continue;
                    }

                    // ✅ Reservation protection:
                    // Only assign this waiting entry if after seating them we still keep enough
                    // FREE seats to satisfy reservation demand.
                    if ((freeSeatsNow - people) < protectedNeed) {
                        // try a smaller waiting party (since we scan 20)
                        continue;
                    }

                    // 2) reserve TABLE(S) for this waiting entry (15-min hold)
                    List<String> tableIds = RestaurantTableDAO.reserveFreeTablesBestFitForWaiting(
                            conn, id, people, HOLD_MINUTES);

                    if (tableIds == null || tableIds.isEmpty()) {
                        continue; // try next waiting entry
                    }

                    // 3) mark WAITING -> ASSIGNED
                    int updated;
                    try (PreparedStatement up = conn.prepareStatement("""
                        UPDATE waiting_list
                        SET status = 'ASSIGNED',
                            request_time = NOW()
                        WHERE waiting_id = ?
                          AND status = 'WAITING'
                    """)) {
                        up.setInt(1, id);
                        updated = up.executeUpdate();
                    }

                    if (updated == 1) {
                        conn.commit();
                        return new WaitingListDTO(id, null, null, null, people, "ASSIGNED", code);
                    } else {
                        // state changed; force rollback of table reservation safely
                        throw new Exception("Waiting list state changed during assignment (id=" + id + ")");
                    }
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


}




