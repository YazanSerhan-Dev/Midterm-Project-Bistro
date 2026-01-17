package DataBase.dao;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.BillDTO;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * Data Access Object (DAO) for handling billing operations.
 *
 * Responsibilities:
 * <ul>
 *   <li>Locate bills by reservation or waiting-list confirmation code</li>
 *   <li>Create bills on demand for active visits</li>
 *   <li>Process bill payments using database transactions</li>
 *   <li>Apply subscriber discounts when applicable</li>
 *   <li>Close visits and release occupied tables after payment</li>
 *   <li>Support reminder queries for unpaid bills</li>
 * </ul>
 *
 * All database access is performed using {@link MySQLConnectionPool}.
 * Transaction safety (commit / rollback) is enforced for payment operations.
 */
public class BillDAO {

    // =========================
    // Existing insert (kept)
    // =========================
    /**
     * Inserts a new bill record for a given visit.
     *
     * @param visitId  the visit identifier
     * @param amount   total bill amount
     * @param discount subscriber discount flag ("YES"/"NO")
     * @param paid     payment status ("YES"/"NO")
     * @throws Exception if database operation fails
     */
    public static void insertBill(int visitId, double amount, String discount, String paid) throws Exception {
        String sql = """
            INSERT INTO bill (visit_id, total_amount, is_subscriber_discount, is_paid)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.setDouble(2, amount);
            ps.setString(3, discount);
            ps.setString(4, paid);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // =========================================================
    // 1) Lookup bill by confirmation code (reservation OR waiting)
    // =========================================================
    /**
     * Retrieves a bill by confirmation code (reservation or waiting list).
     * <p>
     * If no bill exists for the active visit, a new bill is created on demand.
     * Handles cases where the bill is already paid.
     *
     * @param code confirmation code
     * @return lookup result containing bill data or error status
     * @throws Exception if database access fails
     */
    public static BillLookupResult getBillByConfirmationCode(String code) throws Exception {
        if (code == null) code = "";
        code = code.trim();
        if (code.isBlank()) return BillLookupResult.notFound("Empty code.");

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            // 1) Resolve code -> reservation_id or waiting_id
            Integer reservationId = findReservationIdByCode(conn, code);
            Integer waitingId = (reservationId == null) ? findWaitingIdByCode(conn, code) : null;

            if (reservationId == null && waitingId == null) {
                return BillLookupResult.notFound("Code not found.");
            }

            // 2) Resolve -> user_activity row
            UserActivityRow ua = findUserActivity(conn, reservationId, waitingId);
            if (ua == null) {
                return BillLookupResult.notFound("No activity found for this code.");
            }

            // 3) Find ACTIVE visit (Option A: actual_end_time IS NULL)
            VisitRow v = findActiveVisit(conn, ua.activityId);
            if (v == null) {
                // maybe it was already paid / ended; try last visit to show "already paid"
                VisitRow last = findLastVisit(conn, ua.activityId);
                if (last == null) return BillLookupResult.notFound("No visit found for this code.");

                BillRow b2 = findBillByVisit(conn, last.visitId);
                if (b2 != null && "YES".equalsIgnoreCase(b2.isPaid)) {
                    BillDTO dtoPaid = buildBillDTO(ua, last, b2);
                    return BillLookupResult.alreadyPaid(dtoPaid, "Bill already paid.");
                }

                return BillLookupResult.notFound("No active visit for this code (not checked-in or already ended).");
            }

            // 4) Find bill for that visit
            BillRow b = findBillByVisit(conn, v.visitId);
            if (b == null) {
                // ✅ Create bill on-demand (first lookup)
                double subtotal = computeSubtotalForCode(conn, reservationId, waitingId);

                // if you have no pricing model yet, subtotal will be a simple placeholder
                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO bill (visit_id, total_amount, is_subscriber_discount, is_paid)
                    VALUES (?, ?, 'NO', 'NO')
                """)) {
                    ps.setInt(1, v.visitId);
                    ps.setDouble(2, subtotal);
                    ps.executeUpdate();
                }

                // re-fetch
                b = findBillByVisit(conn, v.visitId);
                if (b == null) {
                    return BillLookupResult.notFound("Failed to create bill for this visit.");
                }
            }


            BillDTO dto = buildBillDTO(ua, v, b);

            boolean paid = "YES".equalsIgnoreCase(b.isPaid);
            if (paid) return BillLookupResult.alreadyPaid(dto, "Bill already paid.");

            return BillLookupResult.ok(dto, "Bill found ✅");

        } finally {
            pool.releaseConnection(pc);
        }
    }

    // =========================================================
    // 2) Pay bill by confirmation code (transaction)
    //    - mark bill paid
    //    - end visit (actual_end_time = NOW())
    //    - free table (status = FREE)
    // =========================================================

    /**
     * Processes payment for a bill identified by confirmation code.
     * <p>
     * This method performs a transactional operation:
     * <ul>
     *   <li>Marks the bill as paid</li>
     *   <li>Ends all active visits for the activity</li>
     *   <li>Releases occupied tables</li>
     *   <li>Updates reservation or waiting-list status</li>
     * </ul>
     *
     * @param code confirmation code
     * @return payment result
     * @throws Exception if payment fails or database error occurs
     */
    public static PayBillResult payBillByConfirmationCode(String code) throws Exception {
        if (code == null) code = "";
        code = code.trim();
        if (code.isBlank()) return PayBillResult.fail("Empty code.");

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            Integer reservationId = findReservationIdByCode(conn, code);
            Integer waitingId = (reservationId == null) ? findWaitingIdByCode(conn, code) : null;

            if (reservationId == null && waitingId == null) {
                conn.rollback();
                return PayBillResult.fail("Code not found.");
            }

            // ✅ Enforce correct "can pay" status before doing anything
            if (reservationId != null) {
                try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT status FROM reservation WHERE reservation_id = ? LIMIT 1
                """)) {
                    ps.setInt(1, reservationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return PayBillResult.fail("Reservation not found.");
                        }
                        String st = rs.getString(1);
                        if (!"ARRIVED".equalsIgnoreCase(st)) {
                            conn.rollback();
                            return PayBillResult.fail(
                                    "Pay allowed only after check-in (reservation must be ARRIVED). Current: " + st
                            );
                        }
                    }
                }
            }

            if (waitingId != null) {
                try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT status FROM waiting_list WHERE waiting_id = ? LIMIT 1
                """)) {
                    ps.setInt(1, waitingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return PayBillResult.fail("Waiting list not found.");
                        }
                        String st = rs.getString(1);
                        if (!"ARRIVED".equalsIgnoreCase(st)) {
                            conn.rollback();
                            return PayBillResult.fail(
                                    "Pay allowed only after check-in (waiting list must be ARRIVED). Current: " + st
                            );
                        }
                    }
                }
            }

            UserActivityRow ua = findUserActivity(conn, reservationId, waitingId);
            if (ua == null) {
                conn.rollback();
                return PayBillResult.fail("No activity found for this code.");
            }

            // find any active visit (there may be MANY in Option B)
            VisitRow v = findActiveVisit(conn, ua.activityId);
            if (v == null) {
                conn.rollback();
                return PayBillResult.fail("No active visit found for this code.");
            }

            // ✅ OPTION B SAFE: bill must be found by ACTIVITY, not by a random visit_id
            BillRow b = findLatestBillByActivity(conn, ua.activityId);
            if (b == null) {
                conn.rollback();
                return PayBillResult.fail("Bill not created yet for this activity. Please click 'Find Bill' first.");
            }

            if ("YES".equalsIgnoreCase(b.isPaid)) {
                conn.rollback();
                return PayBillResult.fail("Bill already paid.");
            }

            // compute final amount (10% for subscriber)
            double subtotal = b.totalAmount;
            boolean subscriber = (ua.subscriberUsername != null && !ua.subscriberUsername.isBlank());
            double discount = subscriber ? subtotal * 0.10 : 0.0;
            double total = subtotal - discount;

            // 1) Update bill
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE bill
                SET is_paid = 'YES',
                    is_subscriber_discount = ?,
                    total_amount = ?
                WHERE bill_id = ? AND is_paid = 'NO'
            """)) {
                ps.setString(1, subscriber ? "YES" : "NO");
                ps.setDouble(2, total);
                ps.setInt(3, b.billId);
                int updated = ps.executeUpdate();
                if (updated != 1) {
                    conn.rollback();
                    return PayBillResult.fail("Pay failed (bill state changed).");
                }
            }

            // =========================================================
            // ✅ OPTION B FIX #1: End ALL active visits of this activity
            // =========================================================
            int endedVisits;
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE visit
                SET actual_end_time = NOW()
                WHERE activity_id = ?
                  AND actual_end_time IS NULL
            """)) {
                ps.setInt(1, ua.activityId);
                endedVisits = ps.executeUpdate();
                if (endedVisits <= 0) {
                    conn.rollback();
                    return PayBillResult.fail("Pay failed (no active visits to end).");
                }
            }

            // =========================================================
            // ✅ OPTION B FIX #2: Free ALL tables used by the reservation OR waiting list
            // =========================================================
            String releasedTablesInfo;

            if (reservationId != null) {
                int freed = RestaurantTableDAO.freeOccupiedTablesForReservation(conn, reservationId);
                if (freed <= 0) {
                    conn.rollback();
                    return PayBillResult.fail("Pay failed (no occupied tables found for reservation).");
                }
                releasedTablesInfo = "Released " + freed + " table(s)";
            } else {
                // ✅ Waiting list Option B: free ALL occupied tables for waitingId
                int freed = RestaurantTableDAO.freeOccupiedTablesForWaiting(conn, waitingId);
                if (freed <= 0) {
                    conn.rollback();
                    return PayBillResult.fail("Pay failed (no occupied tables found for waiting list).");
                }
                releasedTablesInfo = "Released " + freed + " table(s)";
            }

            // 4) Finish reservation after payment: ARRIVED -> EXPIRED
            if (reservationId != null) {
                try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE reservation
                    SET status = 'EXPIRED'
                    WHERE reservation_id = ?
                      AND status = 'ARRIVED'
                """)) {
                    ps.setInt(1, reservationId);
                    int updated = ps.executeUpdate();
                    if (updated != 1) {
                        conn.rollback();
                        return PayBillResult.fail("Pay failed (reservation not ARRIVED / already finished).");
                    }
                }
            }

            // 5) Finish waiting list after payment: ARRIVED -> EXPIRED
            if (waitingId != null) {
                try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE waiting_list
                    SET status = 'EXPIRED'
                    WHERE waiting_id = ?
                      AND status = 'ARRIVED'
                """)) {
                    ps.setInt(1, waitingId);
                    int updated = ps.executeUpdate();
                    if (updated != 1) {
                        conn.rollback();
                        return PayBillResult.fail("Pay failed (waiting list not ARRIVED / already finished).");
                    }
                }
            }

            conn.commit();

            // Keep compatibility with UI (tableId still returned, though multi-table exists)
            // For multi-table, we still return the main table from the active visit.
            return PayBillResult.ok("Payment successful ✅ — " + releasedTablesInfo, v.tableId);

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            return PayBillResult.fail("Server error: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }

    // =========================
    // Helpers (private)
    // =========================
    
    private static BillRow findLatestBillByActivity(Connection conn, int activityId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT b.bill_id, b.visit_id, b.total_amount, b.is_subscriber_discount, b.is_paid
            FROM bill b
            JOIN visit v ON v.visit_id = b.visit_id
            WHERE v.activity_id = ?
            ORDER BY b.bill_id DESC
            LIMIT 1
        """)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new BillRow(
                        rs.getInt("bill_id"),
                        rs.getInt("visit_id"),
                        rs.getDouble("total_amount"),
                        rs.getString("is_subscriber_discount"),
                        rs.getString("is_paid")
                );
            }
        }
    }


    private static Integer findReservationIdByCode(Connection conn, String code) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT reservation_id FROM reservation WHERE confirmation_code = ?
        """)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private static Integer findWaitingIdByCode(Connection conn, String code) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT waiting_id FROM waiting_list WHERE confirmation_code = ?
        """)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private static UserActivityRow findUserActivity(Connection conn, Integer reservationId, Integer waitingId) throws Exception {
        if (reservationId != null) {
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT activity_id, subscriber_username, guest_email, guest_phone
                FROM user_activity
                WHERE reservation_id = ?
                ORDER BY activity_date DESC
                LIMIT 1
            """)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return new UserActivityRow(
                            rs.getInt("activity_id"),
                            rs.getString("subscriber_username"),
                            rs.getString("guest_email"),
                            rs.getString("guest_phone")
                    );
                }
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT activity_id, subscriber_username, guest_email, guest_phone
                FROM user_activity
                WHERE waiting_id = ?
                ORDER BY activity_date DESC
                LIMIT 1
            """)) {
                ps.setInt(1, waitingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return new UserActivityRow(
                            rs.getInt("activity_id"),
                            rs.getString("subscriber_username"),
                            rs.getString("guest_email"),
                            rs.getString("guest_phone")
                    );
                }
            }
        }
    }

    private static VisitRow findActiveVisit(Connection conn, int activityId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT visit_id, table_id, actual_start_time, actual_end_time
            FROM visit
            WHERE activity_id = ?
              AND actual_end_time IS NULL
            ORDER BY actual_start_time DESC
            LIMIT 1
        """)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new VisitRow(
                        rs.getInt("visit_id"),
                        rs.getString("table_id"),
                        rs.getTimestamp("actual_start_time"),
                        rs.getTimestamp("actual_end_time")
                );
            }
        }
    }

    private static VisitRow findLastVisit(Connection conn, int activityId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT visit_id, table_id, actual_start_time, actual_end_time
            FROM visit
            WHERE activity_id = ?
            ORDER BY actual_start_time DESC
            LIMIT 1
        """)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new VisitRow(
                        rs.getInt("visit_id"),
                        rs.getString("table_id"),
                        rs.getTimestamp("actual_start_time"),
                        rs.getTimestamp("actual_end_time")
                );
            }
        }
    }

    private static BillRow findBillByVisit(Connection conn, int visitId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT bill_id, visit_id, total_amount, is_subscriber_discount, is_paid
            FROM bill
            WHERE visit_id = ?
            ORDER BY bill_id DESC
            LIMIT 1
        """)) {
            ps.setInt(1, visitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new BillRow(
                        rs.getInt("bill_id"),
                        rs.getInt("visit_id"),
                        rs.getDouble("total_amount"),
                        rs.getString("is_subscriber_discount"),
                        rs.getString("is_paid")
                );
            }
        }
    }

    private static BillDTO buildBillDTO(UserActivityRow ua, VisitRow v, BillRow b) {
        boolean subscriber = (ua.subscriberUsername != null && !ua.subscriberUsername.isBlank());
        double subtotal = b.totalAmount;
        double discount = subscriber ? subtotal * 0.10 : 0.0;
        double total = subtotal - discount;

        String customerName = subscriber ? ua.subscriberUsername : (ua.guestEmail != null && !ua.guestEmail.isBlank() ? ua.guestEmail : "Guest");

        // due date = start + 2 hours (computed)
        String due = "-";
        if (v.start != null) {
            LocalDateTime dueLdt = v.start.toLocalDateTime().plusHours(2);
            due = dueLdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }

        BillDTO dto = new BillDTO();
        dto.setCustomerName(customerName);
        dto.setItemsCount(1);
        dto.setSubtotal(subtotal);
        dto.setDiscount(discount);
        dto.setTotal(total);
        dto.setDueDate(due);
        dto.setSubscriberDiscountApplied(subscriber);

        return dto;
    }
    
    private static double computeSubtotalForCode(Connection conn, Integer reservationId, Integer waitingId) throws Exception {
        final double PRICE_PER_PERSON = 100.0; // <-- change if you want

        if (reservationId != null) {
            try (PreparedStatement ps = conn.prepareStatement("""
                SELECT num_of_customers FROM reservation WHERE reservation_id = ?
            """)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int ppl = rs.getInt(1);
                        return Math.max(0, ppl) * PRICE_PER_PERSON;
                    }
                }
            }
            return 0.0;
        }

        // waiting list
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT num_of_customers FROM waiting_list WHERE waiting_id = ?
        """)) {
            ps.setInt(1, waitingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int ppl = rs.getInt(1);
                    return Math.max(0, ppl) * PRICE_PER_PERSON;
                }
            }
        }
        return 0.0;
    }


    // =========================
    // Small structs
    // =========================
    private record UserActivityRow(int activityId, String subscriberUsername, String guestEmail, String guestPhone) {}
    private record VisitRow(int visitId, String tableId, Timestamp start, Timestamp end) {}
    private record BillRow(int billId, int visitId, double totalAmount, String isSubscriberDiscount, String isPaid) {}

    // =========================
    // Results (simple)
    // =========================

    /**
     * Result wrapper for bill lookup operations.
     */
    public static class BillLookupResult {
        public final boolean ok;
        public final boolean alreadyPaid;
        public final String message;
        public final BillDTO bill;

        private BillLookupResult(boolean ok, boolean alreadyPaid, String message, BillDTO bill) {
            this.ok = ok;
            this.alreadyPaid = alreadyPaid;
            this.message = message;
            this.bill = bill;
        }

        public static BillLookupResult ok(BillDTO bill, String msg) { return new BillLookupResult(true, false, msg, bill); }
        public static BillLookupResult alreadyPaid(BillDTO bill, String msg) { return new BillLookupResult(true, true, msg, bill); }
        public static BillLookupResult notFound(String msg) { return new BillLookupResult(false, false, msg, null); }
    }

    /**
     * Result wrapper for payment operations.
     */
    public static class PayBillResult {
        public final boolean ok;
        public final String message;
        public final String tableId;

        private PayBillResult(boolean ok, String message, String tableId) {
            this.ok = ok;
            this.message = message;
            this.tableId = tableId;
        }

        public static PayBillResult ok(String msg, String tableId) { return new PayBillResult(true, msg, tableId); }
        public static PayBillResult fail(String msg) { return new PayBillResult(false, msg, null); }
    }

    /**
     * Lightweight record used for reminder processing.
     */
    public static class BillReminderRow {
        public final int billId;
        public final String email;
        public final String phone;
        public final String confirmationCode;

        public BillReminderRow(int billId, String email, String phone, String confirmationCode) {
            this.billId = billId;
            this.email = email;
            this.phone = phone;
            this.confirmationCode = confirmationCode;
        }
    }

    public static List<BillReminderRow> findBillsNeedingReminder(int limit) throws Exception {
    	String sql = """
    		    SELECT 
    		        b.bill_id,
    		        COALESCE(s.email, ua.guest_email) AS email,
    		        COALESCE(s.phone, ua.guest_phone) AS phone,
    		        COALESCE(r.confirmation_code, wl.confirmation_code) AS code
    		    FROM bill b
    		    JOIN visit v ON v.visit_id = b.visit_id
    		    JOIN user_activity ua ON ua.activity_id = v.activity_id
    		    LEFT JOIN subscribers s ON s.username = ua.subscriber_username
    		    LEFT JOIN reservation r ON r.reservation_id = ua.reservation_id
    		    LEFT JOIN waiting_list wl ON wl.waiting_id = ua.waiting_id
    		    WHERE b.is_paid = 'NO'
    		      AND b.reminder_sent = 'NO'
    		      AND v.actual_end_time IS NULL
    		      AND v.actual_start_time <= (NOW() - INTERVAL 2 HOUR)
    		      AND COALESCE(s.email, ua.guest_email) IS NOT NULL
    		      AND COALESCE(s.email, ua.guest_email) <> ''
    		      AND COALESCE(r.confirmation_code, wl.confirmation_code) IS NOT NULL
    		      AND COALESCE(r.confirmation_code, wl.confirmation_code) <> ''
    		    ORDER BY v.actual_start_time ASC
    		    LIMIT ?
    		""";


        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);

            List<BillReminderRow> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                	out.add(new BillReminderRow(
                	        rs.getInt("bill_id"),
                	        rs.getString("email"),
                	        rs.getString("phone"),
                	        rs.getString("code")
                	));
                }
            }
            return out;
        } finally {
            pool.releaseConnection(pc);
        }
    }
    /**
     * Marks a bill reminder as sent.
     * <p>
     * Updates the bill record by setting the reminder flag and
     * storing the timestamp of when the reminder was sent.
     * This method is typically used after sending an email or SMS reminder.
     *
     * @param billId the identifier of the bill to update
     * @throws Exception if a database error occurs
     */
    public static void markReminderSent(int billId) throws Exception {
        String sql = """
            UPDATE bill
            SET reminder_sent = 'YES',
                reminder_sent_at = NOW()
            WHERE bill_id = ?
              AND reminder_sent = 'NO'
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

}

