package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class UserActivityDAO {

    public static void insertActivity(
            String subscriberUsername,
            String guestPhone,
            String guestEmail,
            Integer reservationId,
            Integer waitingId,
            Timestamp activityDate) throws Exception {

        String sql = """
            INSERT INTO user_activity
            (subscriber_username, guest_phone, guest_email,
             reservation_id, waiting_id, activity_date)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            // subscriber_username
            if (subscriberUsername == null || subscriberUsername.isEmpty())
                ps.setNull(1, Types.VARCHAR);
            else
                ps.setString(1, subscriberUsername);

            // guest_phone
            if (guestPhone == null || guestPhone.isEmpty())
                ps.setNull(2, Types.VARCHAR);
            else
                ps.setString(2, guestPhone);

            // guest_email
            if (guestEmail == null || guestEmail.isEmpty())
                ps.setNull(3, Types.VARCHAR);
            else
                ps.setString(3, guestEmail);

            // reservation_id
            if (reservationId == null)
                ps.setNull(4, Types.INTEGER);
            else
                ps.setInt(4, reservationId);

            // waiting_id
            if (waitingId == null)
                ps.setNull(5, Types.INTEGER);
            else
                ps.setInt(5, waitingId);

            // activity_date
            ps.setTimestamp(6, activityDate);

            ps.executeUpdate();

        } finally {
            pool.releaseConnection(pc);
        }
    }
 // ADD THIS METHOD inside UserActivityDAO class
    public static void insertActivity(Connection conn,
                                      String subscriberUsername,
                                      String guestPhone,
                                      String guestEmail,
                                      Integer reservationId,
                                      Integer waitingId,
                                      Timestamp activityDate) throws Exception {

        String sql = """
            INSERT INTO user_activity
            (subscriber_username, guest_phone, guest_email,
             reservation_id, waiting_id, activity_date)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            if (subscriberUsername == null || subscriberUsername.isEmpty())
                ps.setNull(1, Types.VARCHAR);
            else
                ps.setString(1, subscriberUsername);

            if (guestPhone == null || guestPhone.isEmpty())
                ps.setNull(2, Types.VARCHAR);
            else
                ps.setString(2, guestPhone);

            if (guestEmail == null || guestEmail.isEmpty())
                ps.setNull(3, Types.VARCHAR);
            else
                ps.setString(3, guestEmail);

            if (reservationId == null)
                ps.setNull(4, Types.INTEGER);
            else
                ps.setInt(4, reservationId);

            if (waitingId == null)
                ps.setNull(5, Types.INTEGER);
            else
                ps.setInt(5, waitingId);

            ps.setTimestamp(6, activityDate);

            ps.executeUpdate();
        }
    }
    
    public static void insertWaitingActivity(int waitingId, String email, String phone) throws Exception {
        String sql = """
            INSERT INTO user_activity (guest_phone, guest_email, waiting_id, activity_date)
            VALUES (?, ?, ?, NOW())
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, email);
            ps.setInt(3, waitingId);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int insertWaitingActivityReturnActivityId(Connection conn, int waitingId, String email, String phone) throws Exception {
        String sql = """
            INSERT INTO user_activity (guest_phone, guest_email, waiting_id, activity_date)
            VALUES (?, ?, ?, NOW())
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, phone);
            ps.setString(2, email);
            ps.setInt(3, waitingId);
            ps.executeUpdate();

            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public static int getLatestActivityIdByReservationId(Connection conn, int reservationId) throws Exception {
        String sql = """
            SELECT activity_id
            FROM user_activity
            WHERE reservation_id = ?
            ORDER BY activity_date DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("activity_id");
            }
        }
        return -1;
    }
    
 // ===============================
 // Lost-code recovery helper
 // ===============================
 public static class LostCodeResult {
     public final String type; // "RESERVATION" or "WAITING"
     public final String code;
     public final String email;

     public LostCodeResult(String type, String code, String email) {
         this.type = type;
         this.code = code;
         this.email = email;
     }
 }

 /**
  * Rule A+ (ASSIGNED only priority):
  * 1) If there is an active WAITING LIST with status = ASSIGNED -> send waiting code (urgent now)
  * 2) Else send the closest upcoming active RESERVATION (CONFIRMED/PENDING/ARRIVED)
  * 3) Else send latest WAITING LIST with status = WAITING
  * 4) Else return null
  *
  * Works for both:
  * - Guests: stored in user_activity.guest_email / guest_phone
  * - Subscribers: stored in subscribers.email / subscribers.phone (user_activity has subscriber_username)
  */
 public static LostCodeResult findActiveCodeByContact(String contact) throws Exception {
     if (contact == null) return null;
     contact = contact.trim();
     if (contact.isEmpty()) return null;

     // 1) Priority: WAITING LIST that is ASSIGNED (ONLY ASSIGNED, not ARRIVED)
     String sqlAssignedWaiting = """
         SELECT
             COALESCE(s.email, ua.guest_email) AS email,
             w.confirmation_code AS code
         FROM user_activity ua
         LEFT JOIN subscribers s ON s.username = ua.subscriber_username
         JOIN waiting_list w ON w.waiting_id = ua.waiting_id
         WHERE (
               ua.guest_email = ? OR ua.guest_phone = ?
            OR s.email = ? OR s.phone = ?
         )
           AND w.status = 'ASSIGNED'
         ORDER BY w.request_time DESC
         LIMIT 1
     """;

     // 2) Closest upcoming active reservation (scheduled)
     String sqlClosestReservation = """
         SELECT
             COALESCE(s.email, ua.guest_email) AS email,
             r.confirmation_code AS code
         FROM user_activity ua
         LEFT JOIN subscribers s ON s.username = ua.subscriber_username
         JOIN reservation r ON r.reservation_id = ua.reservation_id
         WHERE (
               ua.guest_email = ? OR ua.guest_phone = ?
            OR s.email = ? OR s.phone = ?
         )
           AND r.status IN ('CONFIRMED','PENDING','ARRIVED')
         ORDER BY
           CASE WHEN r.reservation_time >= NOW() THEN 0 ELSE 1 END,
           ABS(TIMESTAMPDIFF(SECOND, r.reservation_time, NOW())) ASC
         LIMIT 1
     """;

     // 3) Fallback: latest waiting list still WAITING
     String sqlLatestWaiting = """
         SELECT
             COALESCE(s.email, ua.guest_email) AS email,
             w.confirmation_code AS code
         FROM user_activity ua
         LEFT JOIN subscribers s ON s.username = ua.subscriber_username
         JOIN waiting_list w ON w.waiting_id = ua.waiting_id
         WHERE (
               ua.guest_email = ? OR ua.guest_phone = ?
            OR s.email = ? OR s.phone = ?
         )
           AND w.status = 'WAITING'
         ORDER BY w.request_time DESC
         LIMIT 1
     """;

     MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
     PooledConnection pc = pool.getConnection();
     Connection conn = pc.getConnection();

     try {
         // Step 1: ASSIGNED waiting list
         LostCodeResult r1 = runLostCodeQuery(conn, sqlAssignedWaiting, contact, "WAITING");
         if (r1 != null) return r1;

         // Step 2: closest reservation
         LostCodeResult r2 = runLostCodeQuery(conn, sqlClosestReservation, contact, "RESERVATION");
         if (r2 != null) return r2;

         // Step 3: latest WAITING list
         LostCodeResult r3 = runLostCodeQuery(conn, sqlLatestWaiting, contact, "WAITING");
         return r3;

     } finally {
         pool.releaseConnection(pc);
     }
 }

 /** Small helper: same param bound 4 times (guest + subscriber search) */
 private static LostCodeResult runLostCodeQuery(Connection conn, String sql, String contact, String type) throws Exception {
     try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setString(1, contact);
         ps.setString(2, contact);
         ps.setString(3, contact);
         ps.setString(4, contact);

         try (ResultSet rs = ps.executeQuery()) {
             if (!rs.next()) return null;

             String email = rs.getString("email");
             String code = rs.getString("code");

             if (code == null || code.isBlank()) return null;

             return new LostCodeResult(type, code.trim(), (email == null ? null : email.trim()));
         }
     }
 }

}
