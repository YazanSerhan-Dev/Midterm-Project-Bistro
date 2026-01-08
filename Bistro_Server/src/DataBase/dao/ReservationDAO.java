package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import DataBase.Reservation;
import Server.EmailService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import common.dto.MakeReservationRequestDTO;

public class ReservationDAO {

    public static void insertReservation(
            int numOfCustomers,
            Timestamp reservationTime,
            Timestamp expiryTime,
            String status,
            String confirmationCode) throws Exception {

        String sql = """
            INSERT INTO reservation
            (num_of_customers, reservation_time, expiry_time, status, confirmation_code)
            VALUES (?, ?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numOfCustomers);
            ps.setTimestamp(2, reservationTime);
            ps.setTimestamp(3, expiryTime);
            ps.setString(4, status);
            ps.setString(5, confirmationCode);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public List<Reservation> getAllReservations() throws SQLException {

        String sql = "SELECT reservation_id, num_of_customers, reservation_time, expiry_time, status, confirmation_code " +
                     "FROM reservation";

        List<Reservation> list = new ArrayList<>();

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("reservation_id");
                int guests = rs.getInt("num_of_customers");
                Timestamp resTime = rs.getTimestamp("reservation_time");
                Timestamp expTime = rs.getTimestamp("expiry_time");
                String status = rs.getString("status");
                String code = rs.getString("confirmation_code");

                list.add(new Reservation(id, guests, resTime, expTime, status, code));
            }

        } finally {
            pool.releaseConnection(pc);
        }

        return list;
    }
 // ADD inside ReservationDAO class
    public static class CreateReservationResult {
        public final int reservationId;
        public final String confirmationCode;

        public CreateReservationResult(int reservationId, String confirmationCode) {
            this.reservationId = reservationId;
            this.confirmationCode = confirmationCode;
        }
    }

    // ADD inside ReservationDAO class
 // inside ReservationDAO class
    public CreateReservationResult createReservationWithActivity(MakeReservationRequestDTO req) throws Exception {

        String status = "CONFIRMED";

        String code = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase();

        Timestamp reservationTime = req.getReservationTime();

        // Dining window = 2 hours (matches your ARRIVED expiry logic)
        Timestamp expiryTime = Timestamp.valueOf(
                reservationTime.toLocalDateTime().plusMinutes(120)
        );

        String insertReservationSql = """
            INSERT INTO reservation
            (num_of_customers, reservation_time, expiry_time, status, confirmation_code)
            VALUES (?, ?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            // ✅ Capacity check BEFORE inserting
            // Total restaurant capacity (you said you already have getAllSeats)
            // If your getAllSeats requires conn, use RestaurantTableDAO.getAllSeats(conn)
            int totalSeats = RestaurantTableDAO.getTotalSeats();

            int booked = getBookedCustomersInRange(conn, reservationTime, expiryTime);

            if (booked + req.getNumOfCustomers() > totalSeats) {
                conn.rollback();
                throw new Exception("No availability at that time (capacity reached).");
            }

            int reservationId;
            try (PreparedStatement ps = conn.prepareStatement(insertReservationSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, req.getNumOfCustomers());
                ps.setTimestamp(2, reservationTime);
                ps.setTimestamp(3, expiryTime);
                ps.setString(4, status);
                ps.setString(5, code);

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("No reservation_id generated.");
                    reservationId = rs.getInt(1);
                }
            }

            // Insert matching row in user_activity (same transaction)
            Timestamp now = new Timestamp(System.currentTimeMillis());

            UserActivityDAO.insertActivity(
                    conn,
                    req.isSubscriber() ? req.getSubscriberUsername() : null,
                    req.isSubscriber() ? null : req.getGuestPhone(),
                    req.isSubscriber() ? null : req.getGuestEmail(),
                    reservationId,
                    null,
                    now
            );

            conn.commit();
            return new CreateReservationResult(reservationId, code);

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;

        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }


    
 // Counts total guests already booked in a time window.
 // We only count statuses that actually occupy capacity: CONFIRMED + ARRIVED.
 public static int getBookedCustomersInRange(Connection conn, Timestamp start, Timestamp end) throws Exception {

     String sql = """
         SELECT COALESCE(SUM(num_of_customers), 0) AS booked
         FROM reservation
         WHERE status IN ('CONFIRMED', 'ARRIVED')
           AND reservation_time < ?
           AND expiry_time > ?
     """;

     try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setTimestamp(1, end);
         ps.setTimestamp(2, start);

         try (ResultSet rs = ps.executeQuery()) {
             return rs.next() ? rs.getInt("booked") : 0;
         }
     }
 }

 // Keep your old signature (opens its own connection) – calls the safe version above.
 public static int getBookedCustomersInRange(Timestamp start, Timestamp end) throws Exception {
     MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
     PooledConnection pc = pool.getConnection();
     Connection conn = pc.getConnection();

     try {
         return getBookedCustomersInRange(conn, start, end);
     } finally {
         pool.releaseConnection(pc);
     }
 }

    
    public static int expireFinishedArrived2Hours() throws Exception {
        String sql = """
            UPDATE reservation
            SET status = 'EXPIRED'
            WHERE status = 'ARRIVED'
              AND reservation_time < (NOW() - INTERVAL 2 HOUR)
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

    
    public static int cancelNoShows15Min() throws Exception {
        String sql = """
            UPDATE reservation
            SET status = 'CANCELED'
            WHERE status = 'CONFIRMED'
              AND reservation_time < (NOW() - INTERVAL 15 MINUTE)
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


 // inside DataBase.dao.ReservationDAO
    public static String markArrivedByCodeReturnTableId(String code) throws Exception {

        String sqlGet = """
            SELECT reservation_id, num_of_customers, status, reservation_time
            FROM reservation
            WHERE confirmation_code = ?
            LIMIT 1
        """;

        String sqlGetDbNow = "SELECT NOW() AS db_now";

        String sqlSetArrived = """
            UPDATE reservation
            SET status = 'ARRIVED',
                expiry_time = DATE_ADD(reservation_time, INTERVAL 2 HOUR)
            WHERE reservation_id = ?
              AND status = 'CONFIRMED'
        """;

        String sqlCancelLate = """
            UPDATE reservation
            SET status = 'CANCELED'
            WHERE reservation_id = ?
              AND status = 'CONFIRMED'
        """;

        String sqlGetActivityId = """
            SELECT activity_id
            FROM user_activity
            WHERE reservation_id = ?
            ORDER BY activity_id DESC
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            if (code == null || code.trim().isEmpty()) {
                throw new Exception("Invalid confirmation code.");
            }

            // 1) Load reservation by code
            int reservationId;
            int numCustomers;
            String status;
            Timestamp reservationTime;

            try (PreparedStatement ps = conn.prepareStatement(sqlGet)) {
                ps.setString(1, code.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new Exception("Invalid confirmation code.");
                    }
                    reservationId = rs.getInt("reservation_id");
                    numCustomers = rs.getInt("num_of_customers");
                    status = rs.getString("status");
                    reservationTime = rs.getTimestamp("reservation_time");
                }
            }

            if (!"CONFIRMED".equalsIgnoreCase(status)) {
                throw new Exception("Check-in not allowed: status is " + status);
            }

            // ✅ ADD THIS
            if (VisitDAO.existsVisitForReservationId(conn, reservationId)) {
                throw new Exception("Already checked-in for this reservation.");
            }

            // 2) Use DB clock to avoid timezone mismatch
            Timestamp dbNow;
            try (PreparedStatement ps = conn.prepareStatement(sqlGetDbNow);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                dbNow = rs.getTimestamp("db_now");
            }

            if (reservationTime == null) {
                throw new Exception("Reservation time is missing.");
            }

            long nowMs = dbNow.getTime();
            long startMs = reservationTime.getTime();
            long endMs = startMs + 15L * 60L * 1000L;

            if (nowMs < startMs) {
                throw new Exception("Too early. Check-in starts at reservation time.");
            }

            if (nowMs > endMs) {
                // cancel late no-show immediately
                try (PreparedStatement ps = conn.prepareStatement(sqlCancelLate)) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }
                conn.commit();
                throw new Exception("Too late. Reservation was canceled (no-show).");
            }

            // 3) Find activity_id (must exist for Visit)
            Integer activityId = null;
            try (PreparedStatement ps = conn.prepareStatement(sqlGetActivityId)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) activityId = rs.getInt("activity_id");
                }
            }

            if (activityId == null) {
                throw new Exception("Missing user_activity row for this reservation. (Fix make-reservation flow)");
            }

            // 4) Allocate table
            String tableId = DataBase.dao.RestaurantTableDAO.allocateFreeTable(conn, numCustomers);
            if (tableId == null) {
                throw new Exception("No suitable FREE table available right now.");
            }

            // 5) Set ARRIVED
            int updated;
            try (PreparedStatement ps = conn.prepareStatement(sqlSetArrived)) {
                ps.setInt(1, reservationId);
                updated = ps.executeUpdate();
            }

            if (updated != 1) {
                throw new Exception("Failed to mark ARRIVED (status changed).");
            }

            // 6) Insert visit
            DataBase.dao.VisitDAO.insertVisit(conn, activityId, tableId);

            conn.commit();
            return tableId;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;

        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }



    public static class ReservationInfo {
        public final int reservationId;
        public final int numOfCustomers;
        public final java.sql.Timestamp reservationTime;
        public final java.sql.Timestamp expiryTime;
        public final String status;
        public final String confirmationCode;
        public final boolean checkInAllowed;

        public ReservationInfo(int reservationId, int numOfCustomers,
                               java.sql.Timestamp reservationTime, java.sql.Timestamp expiryTime,
                               String status, String confirmationCode,
                               boolean checkInAllowed) {
            this.reservationId = reservationId;
            this.numOfCustomers = numOfCustomers;
            this.reservationTime = reservationTime;
            this.expiryTime = expiryTime;
            this.status = status;
            this.confirmationCode = confirmationCode;
            this.checkInAllowed = checkInAllowed;
        }
    }

    public static ReservationInfo getReservationInfoByCode(String code) throws Exception {
        String sql = """
            SELECT reservation_id, num_of_customers, reservation_time, expiry_time, status, confirmation_code
            FROM reservation
            WHERE confirmation_code = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        java.sql.Connection conn = pc.getConnection();

        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                int reservationId = rs.getInt("reservation_id");
                int num = rs.getInt("num_of_customers");
                java.sql.Timestamp resTime = rs.getTimestamp("reservation_time");
                java.sql.Timestamp expTime = rs.getTimestamp("expiry_time");
                String status = rs.getString("status");
                String conf = rs.getString("confirmation_code");

                // check-in allowed ONLY if status CONFIRMED and now within [time, time+15min]
                boolean allowed = false;
                if (resTime != null && "CONFIRMED".equalsIgnoreCase(status)) {
                    long now = System.currentTimeMillis();
                    long start = resTime.getTime();
                    long end15 = start + 15L * 60L * 1000L;
                    allowed = (now >= start && now <= end15);
                }

                return new ReservationInfo(reservationId, num, resTime, expTime, status, conf, allowed);
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static common.dto.TerminalValidateResponseDTO getTerminalInfoByCode(String code) throws Exception {

        String sql = """
            SELECT
                r.reservation_id,
                r.reservation_time,
                r.num_of_customers,
                r.status,
                CASE
                  WHEN r.status = 'CONFIRMED'
                   AND NOW() >= r.reservation_time
                   AND NOW() <= DATE_ADD(r.reservation_time, INTERVAL 15 MINUTE)
                  THEN 1 ELSE 0
                END AS can_check_in
            FROM reservation r
            WHERE r.confirmation_code = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code == null ? "" : code.trim());

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return new common.dto.TerminalValidateResponseDTO(false, "Invalid code.");
                }

                int reservationId = rs.getInt("reservation_id");
                java.sql.Timestamp time = rs.getTimestamp("reservation_time");
                int num = rs.getInt("num_of_customers");
                String status = rs.getString("status");
                boolean canCheckIn = rs.getInt("can_check_in") == 1;

                common.dto.TerminalValidateResponseDTO dto = new common.dto.TerminalValidateResponseDTO(true, "OK");
                dto.setReservationId(reservationId);
                dto.setReservationTime(time);
                dto.setNumOfCustomers(num);
                dto.setStatus(status == null ? "" : status);

                // Default
                dto.setTableId("-");
                dto.setCheckInAllowed(false);

                // Case 1: normal confirmed window
                if ("CONFIRMED".equalsIgnoreCase(status)) {
                    dto.setCheckInAllowed(canCheckIn);
                    dto.setMessage(canCheckIn ? "Code valid. Ready to check-in." : "Too early/late for check-in (allowed from reservation time until +15 minutes).");
                    return dto;
                }

                // Case 2: waiting-for-table (PENDING)
                if ("PENDING".equalsIgnoreCase(status)) {
                    // If auto-reserved happened, show table + allow confirm scan
                    Integer visitId = VisitDAO.getLatestVisitIdForReservation(conn, reservationId);
                    String tableId = VisitDAO.getLatestTableIdForReservation(conn, reservationId);

                    if (visitId != null && tableId != null && PerformanceLogDAO.isAutoReservedNotConfirmed(conn, visitId)) {
                        dto.setTableId(tableId);
                        dto.setCheckInAllowed(true);
                        dto.setMessage("Table is ready: " + tableId + ". Press Check-in to confirm within 15 minutes.");
                    } else {
                        dto.setCheckInAllowed(false);
                        dto.setMessage("No suitable table right now. Please wait — you will be notified when a table is ready.");
                    }
                    return dto;
                }

                // Case 3: already arrived
                if ("ARRIVED".equalsIgnoreCase(status)) {
                    dto.setCheckInAllowed(false);
                    dto.setMessage("Already arrived.");
                    return dto;
                }

                // Other statuses
                dto.setCheckInAllowed(false);
                dto.setMessage("Reservation status: " + status);
                return dto;
            }

        } finally {
            pool.releaseConnection(pc);
        }
    }



 // inside ReservationDAO
    public static boolean canFitAtTime(
            Timestamp start,
            int numCustomers
    ) throws Exception {

        Timestamp end = Timestamp.valueOf(
                start.toLocalDateTime().plusMinutes(120)
        );

        int totalSeats = RestaurantTableDAO.getTotalSeats();
        int booked = getBookedCustomersInRange(start, end);

        return booked + numCustomers <= totalSeats;
    }

    public static String getReservationEmail(int reservationId) throws Exception {
        String sql = """
            SELECT COALESCE(s.email, ua.guest_email) AS email
            FROM user_activity ua
            LEFT JOIN subscribers s ON s.username = ua.subscriber_username
            WHERE ua.reservation_id = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        java.sql.Connection conn = pc.getConnection();

        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String email = rs.getString("email");
                    return (email == null || email.isBlank()) ? null : email.trim();
                }
                return null;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static String getReservationPhone(int reservationId) throws Exception {
        String sql = """
            SELECT COALESCE(s.phone, ua.guest_phone) AS phone
            FROM user_activity ua
            LEFT JOIN subscribers s ON s.username = ua.subscriber_username
            WHERE ua.reservation_id = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        java.sql.Connection conn = pc.getConnection();

        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String phone = rs.getString("phone");
                    return (phone == null || phone.isBlank()) ? null : phone.trim();
                }
                return null;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public List<Reservation> getReservationsBySubscriber(String username) throws Exception {

        String sql = """
            SELECT r.reservation_id, r.num_of_customers, r.reservation_time,
                   r.expiry_time, r.status, r.confirmation_code
            FROM reservation r
            JOIN user_activity ua ON ua.reservation_id = r.reservation_id
            WHERE ua.subscriber_username = ?
            ORDER BY r.reservation_time DESC
        """;

        List<Reservation> list = new ArrayList<>();

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Reservation(
                    rs.getInt("reservation_id"),
                    rs.getInt("num_of_customers"),
                    rs.getTimestamp("reservation_time"),
                    rs.getTimestamp("expiry_time"),
                    rs.getString("status"),
                    rs.getString("confirmation_code")
                ));
            }
        } finally {
            pool.releaseConnection(pc);
        }

        return list;
    }

    public List<Reservation> getReservationsByGuest(String email, String phone) throws Exception {

        String sql = """
            SELECT r.reservation_id, r.num_of_customers, r.reservation_time,
                   r.expiry_time, r.status, r.confirmation_code
            FROM reservation r
            JOIN user_activity ua ON ua.reservation_id = r.reservation_id
            WHERE (ua.guest_email = ? OR ua.guest_phone = ?)
            ORDER BY r.reservation_time DESC
        """;

        List<Reservation> list = new ArrayList<>();

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, phone);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Reservation(
                    rs.getInt("reservation_id"),
                    rs.getInt("num_of_customers"),
                    rs.getTimestamp("reservation_time"),
                    rs.getTimestamp("expiry_time"),
                    rs.getString("status"),
                    rs.getString("confirmation_code")
                ));
            }
        } finally {
            pool.releaseConnection(pc);
        }

        return list;
    }
    
    /**
     * Cancel a reservation (set status to CANCELLED) only if:
     * - it belongs to the requester (subscriber_username OR guest_email+guest_phone in user_activity)
     * - and reservation is still cancellable (currently CONFIRMED)
     *
     * @return true if the reservation was updated, false otherwise
     */
    public static boolean cancelReservation(int reservationId, String role, String username, String guestEmail, String guestPhone) throws Exception {
        if (role == null) return false;
        String r = role.trim().toUpperCase();

        if ("SUBSCRIBER".equals(r)) {
            return cancelReservationForSubscriber(reservationId, username);
        } else {
            // treat anything else as CUSTOMER/GUEST
            return cancelReservationForGuest(reservationId, guestEmail, guestPhone);
        }
    }

    private static boolean cancelReservationForSubscriber(int reservationId, String username) throws Exception {
        if (reservationId <= 0) return false;
        if (username == null || username.isBlank()) return false;

        String sql = """
            UPDATE reservation r
            JOIN user_activity ua ON ua.reservation_id = r.reservation_id
            SET r.status = 'CANCELED'
            WHERE r.reservation_id = ?
              AND r.status = 'CONFIRMED'
              AND ua.subscriber_username = ?
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setString(2, username);
            int updated = ps.executeUpdate();
            return updated > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    private static boolean cancelReservationForGuest(int reservationId, String email, String phone) throws Exception {
        if (reservationId <= 0) return false;
        if (email == null || email.isBlank()) return false;
        if (phone == null || phone.isBlank()) return false;

        String sql = """
            UPDATE reservation r
            JOIN user_activity ua ON ua.reservation_id = r.reservation_id
            SET r.status = 'CANCELED'
            WHERE r.reservation_id = ?
              AND r.status = 'CONFIRMED'
              AND ua.guest_email = ?
              AND ua.guest_phone = ?
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setString(2, email);
            ps.setString(3, phone);
            int updated = ps.executeUpdate();
            return updated > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int getDueReservationSeatsNow() throws Exception {

        String sql = """
            SELECT COALESCE(SUM(num_of_customers),0) AS total
            FROM reservation
            WHERE status = 'CONFIRMED'
              AND NOW() >= reservation_time
              AND NOW() <= DATE_ADD(reservation_time, INTERVAL 15 MINUTE)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt("total");
            return 0;

        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static boolean markPendingByCode(Connection conn, String code) throws Exception {
        String sql = """
            UPDATE reservation
            SET status = 'PENDING'
            WHERE confirmation_code = ?
              AND status = 'CONFIRMED'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code == null ? "" : code.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public static class ResMini {
        public final int reservationId;
        public final int numCustomers;
        public final String status;

        public ResMini(int reservationId, int numCustomers, String status) {
            this.reservationId = reservationId;
            this.numCustomers = numCustomers;
            this.status = status;
        }
    }

    public static ResMini getReservationMiniByCode(Connection conn, String code) throws Exception {
        String sql = """
            SELECT reservation_id, num_of_customers, status
            FROM reservation
            WHERE confirmation_code = ?
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code == null ? "" : code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ResMini(
                    rs.getInt("reservation_id"),
                    rs.getInt("num_of_customers"),
                    rs.getString("status")
                );
            }
        }
    }

    public static void autoReserveForPendingReservations() throws Exception {
        String pickSql = """
            SELECT reservation_id, num_of_customers
            FROM reservation
            WHERE status = 'PENDING'
            ORDER BY reservation_time ASC
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            Integer resId = null;
            Integer seats = null;

            try (PreparedStatement ps = conn.prepareStatement(pickSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resId = rs.getInt("reservation_id");
                    seats = rs.getInt("num_of_customers");
                }
            }

            if (resId == null) {
                conn.rollback();
                return;
            }

            // Do we already have a RESERVED table for this reservation? (avoid duplicates)
            String existingReserved = VisitDAO.getLatestTableIdForReservation(conn, resId);
            if (existingReserved != null) {
                // if table currently RESERVED, don’t create another one
                conn.rollback();
                return;
            }

            String tableId = RestaurantTableDAO.reserveFreeTable(conn, seats);
            if (tableId == null) {
                conn.rollback();
                return;
            }

            int activityId = UserActivityDAO.getLatestActivityIdByReservationId(conn, resId);
            if (activityId <= 0) {
                // no activity found -> revert table reserve
                RestaurantTableDAO.freeTable(conn, tableId);
                conn.rollback();
                return;
            }

            int visitId = VisitDAO.insertVisitReturnVisitId(conn, activityId, tableId);
            if (visitId <= 0) {
                RestaurantTableDAO.freeTable(conn, tableId);
                conn.rollback();
                return;
            }

            PerformanceLogDAO.insertAutoReservedNotConfirmed(conn, visitId);

            conn.commit();

            // After commit -> send notification (email/sms)
            try {
                String email = getReservationEmail(resId); // you already have this method in ReservationDAO
                if (email != null && !email.isBlank()) {
                    EmailService.sendReservationTableReady(email, tableId);
                }
                // if you have phone method, also send SMS
            } catch (Exception ignored) {}

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }

    public static common.dto.TerminalValidateResponseDTO confirmAutoReservedByCode(String code) throws Exception {

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            ResMini r = getReservationMiniByCode(conn, code);
            if (r == null) {
                conn.rollback();
                return new common.dto.TerminalValidateResponseDTO(false, "Invalid code.");
            }

            // Only for PENDING reservations
            if (!"PENDING".equalsIgnoreCase(r.status)) {
                conn.rollback();
                return null; // let normal check-in flow handle it
            }

            Integer visitId = VisitDAO.getLatestVisitIdForReservation(conn, r.reservationId);
            String tableId = VisitDAO.getLatestTableIdForReservation(conn, r.reservationId);

            if (visitId == null || tableId == null) {
                conn.rollback();
                return null;
            }

            // Must be auto-reserved-not-confirmed
            if (!PerformanceLogDAO.isAutoReservedNotConfirmed(conn, visitId)) {
                conn.rollback();
                return null;
            }

            // Table must be RESERVED
            boolean occupied = RestaurantTableDAO.occupyReservedTable(conn, tableId);
            if (!occupied) {
                conn.rollback();
                return new common.dto.TerminalValidateResponseDTO(false, "Reserved table is no longer available.");
            }

            // Reservation -> ARRIVED
            try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE reservation
                SET status = 'ARRIVED'
                WHERE reservation_id = ? AND status = 'PENDING'
            """)) {
                ps.setInt(1, r.reservationId);
                ps.executeUpdate();
            }

            PerformanceLogDAO.confirmAutoReserved(conn, visitId);

            conn.commit();

            common.dto.TerminalValidateResponseDTO dto = new common.dto.TerminalValidateResponseDTO(true, "Confirmed. Enjoy your meal ✅");
            dto.setReservationId(r.reservationId);
            dto.setNumOfCustomers(r.numCustomers);
            dto.setStatus("ARRIVED");
            dto.setCheckInAllowed(false);
            dto.setTableId(tableId);
            return dto;

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }

    public static void releaseExpiredAutoReserved(int minutes) throws Exception {

        String sql = """
            SELECT v.visit_id, v.table_id, r.reservation_id
            FROM visit v
            JOIN performance_log pl ON pl.visit_id = v.visit_id
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            JOIN reservation r ON r.reservation_id = ua.reservation_id
            JOIN restaurant_table t ON t.table_id = v.table_id
            WHERE t.status = 'RESERVED'
              AND pl.late_minutes = -999
              AND v.actual_start_time < (NOW() - INTERVAL ? MINUTE)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, minutes);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int visitId = rs.getInt("visit_id");
                        String tableId = rs.getString("table_id");
                        int reservationId = rs.getInt("reservation_id");

                        // free table
                        RestaurantTableDAO.freeTable(conn, tableId);

                        // delete marker + visit
                        PerformanceLogDAO.deleteByVisitId(conn, visitId);
                        VisitDAO.deleteVisitById(conn, visitId);

                        // reservation stays PENDING (still waiting)
                        // (no update needed)
                    }
                }
            }

            conn.commit();

        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            pool.releaseConnection(pc);
        }
    }


}
