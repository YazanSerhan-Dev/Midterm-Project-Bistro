package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.TerminalActiveItemDTO;
/**
 * Data Access Object for user activity tracking.
 * <p>
 * Manages all activity records related to subscribers and guests,
 * including reservations, waiting list entries, terminal lookups,
 * and lost confirmation-code recovery.
 */
public class UserActivityDAO {
	/**
	 * Inserts a new user activity record.
	 * <p>
	 * Used when creating a reservation or waiting-list entry.
	 * Supports both subscribers and guests (nullable fields allowed).
	 *
	 * @param subscriberUsername subscriber username (nullable)
	 * @param guestPhone guest phone number (nullable)
	 * @param guestEmail guest email address (nullable)
	 * @param reservationId linked reservation ID (nullable)
	 * @param waitingId linked waiting-list ID (nullable)
	 * @param activityDate activity timestamp
	 * @throws Exception on database error
	 */
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
    /**
     * Inserts a user activity record using an existing connection.
     * <p>
     * Intended for transactional flows where activity creation
     * must be committed or rolled back together with other operations.
     *
     * @param conn active database connection
     * @param subscriberUsername subscriber username (nullable)
     * @param guestPhone guest phone (nullable)
     * @param guestEmail guest email (nullable)
     * @param reservationId reservation ID (nullable)
     * @param waitingId waiting-list ID (nullable)
     * @param activityDate activity timestamp
     * @throws Exception on database error
     */
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
    /**
     * Inserts a waiting-list activity for a guest.
     *
     * @param waitingId waiting-list ID
     * @param email guest email
     * @param phone guest phone
     * @throws Exception on database error
     */
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
    /**
     * Inserts a waiting-list activity for a subscriber.
     *
     * @param waitingId waiting-list ID
     * @param subscriberUsername subscriber username
     * @throws Exception on database error
     */
    public static void insertSubscriberWaitingActivity(int waitingId, String subscriberUsername) throws Exception {
        String sql = """
            INSERT INTO user_activity (subscriber_username, waiting_id, activity_date)
            VALUES (?, ?, NOW())
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subscriberUsername);
            ps.setInt(2, waitingId);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    /**
     * Inserts a waiting-list activity and returns the generated activity ID.
     * <p>
     * Used when the caller needs to link further records to this activity.
     *
     * @param conn active database connection
     * @param waitingId waiting-list ID
     * @param email guest email
     * @param phone guest phone
     * @return generated activity ID, or -1 if not available
     * @throws Exception on database error
     */
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

 // ===============================
 // Lost-code recovery helper
 // ===============================
    /**
     * Result holder for lost confirmation-code recovery.
     * <p>
     * Represents either a reservation or waiting-list code
     * associated with a specific contact.
     */
    public static class LostCodeResult {
        public final String type;  // "RESERVATION" or "WAITING"
        public final String code;
        public final String email;
        public final String phone;

        public LostCodeResult(String type, String code, String email, String phone) {
            this.type = type;
            this.code = code;
            this.email = email;
            this.phone = phone;
        }
    }

    /**
     * Finds the most relevant active confirmation code by contact details.
     * <p>
     * Priority order:
     * 1) ASSIGNED waiting list (urgent)
     * 2) Closest upcoming active reservation
     * 3) Latest waiting-list entry still WAITING
     *
     * Works for both guests and subscribers.
     *
     * @param contact email or phone number
     * @return LostCodeResult or null if nothing active found
     * @throws Exception on database error
     */
 public static LostCodeResult findActiveCodeByContact(String contact) throws Exception {
     if (contact == null) return null;
     contact = contact.trim();
     if (contact.isEmpty()) return null;

     // 1) Priority: WAITING LIST that is ASSIGNED (ONLY ASSIGNED, not ARRIVED)
     String sqlAssignedWaiting = """
    		    SELECT
    		        COALESCE(s.email, ua.guest_email) AS email,
    		        COALESCE(s.phone, ua.guest_phone) AS phone,
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
    		        COALESCE(s.phone, ua.guest_phone) AS phone,
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
    		        COALESCE(s.phone, ua.guest_phone) AS phone,
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
             String phone = rs.getString("phone");
             String code  = rs.getString("code");

             if (code == null || code.isBlank()) return null;

             return new LostCodeResult(
                 type,
                 code.trim(),
                 (email == null ? null : email.trim()),
                 (phone == null ? null : phone.trim())
             );
         }
     }
 }
 /**
  * Simple holder for an active confirmation code.
  * <p>
  * Used mainly for terminal and subscriber quick-lookup flows.
  */
 public static class ActiveCodeResult {
	    public final String type;  // "RESERVATION" or "WAITING_LIST"
	    public final String code;  // confirmation_code

	    public ActiveCodeResult(String type, String code) {
	        this.type = type;
	        this.code = code;
	    }
	}
 /**
  * Finds the most relevant active confirmation code for a subscriber.
  * <p>
  * Priority:
  * 1) ASSIGNED waiting list
  * 2) Closest upcoming active reservation
  * 3) Latest waiting-list entry
  *
  * @param subscriberUsername subscriber username
  * @return ActiveCodeResult or null if none found
  * @throws Exception on database error
  */
 public static ActiveCodeResult findActiveCodeBySubscriberUsername(String subscriberUsername) throws Exception {

	    // 1) Priority: ASSIGNED waiting list (urgent now)
	    String sqlAssignedWaiting = """
	        SELECT w.confirmation_code
	        FROM user_activity ua
	        JOIN waiting_list w ON w.waiting_id = ua.waiting_id
	        WHERE ua.subscriber_username = ?
	          AND ua.waiting_id IS NOT NULL
	          AND w.status = 'ASSIGNED'
	        ORDER BY w.request_time DESC
	        LIMIT 1
	    """;

	    // 2) Closest upcoming active reservation
	    String sqlClosestReservation = """
	        SELECT r.confirmation_code
	        FROM user_activity ua
	        JOIN reservation r ON r.reservation_id = ua.reservation_id
	        WHERE ua.subscriber_username = ?
	          AND ua.reservation_id IS NOT NULL
	          AND r.status IN ('CONFIRMED','PENDING')
	        ORDER BY
	          CASE WHEN r.reservation_time >= NOW() THEN 0 ELSE 1 END,
	          ABS(TIMESTAMPDIFF(SECOND, r.reservation_time, NOW())) ASC
	        LIMIT 1
	    """;

	    // 3) Fallback: latest WAITING waiting list
	    String sqlLatestWaiting = """
	        SELECT w.confirmation_code
	        FROM user_activity ua
	        JOIN waiting_list w ON w.waiting_id = ua.waiting_id
	        WHERE ua.subscriber_username = ?
	          AND ua.waiting_id IS NOT NULL
	          AND w.status = 'WAITING'
	        ORDER BY w.request_time DESC
	        LIMIT 1
	    """;

	    MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
	    PooledConnection pc = pool.getConnection();
	    Connection conn = pc.getConnection();

	    try {
	        // Step 1: ASSIGNED waiting list
	        try (PreparedStatement ps = conn.prepareStatement(sqlAssignedWaiting)) {
	            ps.setString(1, subscriberUsername);
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    String code = rs.getString("confirmation_code");
	                    if (code != null && !code.isBlank()) {
	                        return new ActiveCodeResult("WAITING_LIST", code.trim());
	                    }
	                }
	            }
	        }

	        // Step 2: closest reservation
	        try (PreparedStatement ps = conn.prepareStatement(sqlClosestReservation)) {
	            ps.setString(1, subscriberUsername);
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    String code = rs.getString("confirmation_code");
	                    if (code != null && !code.isBlank()) {
	                        return new ActiveCodeResult("RESERVATION", code.trim());
	                    }
	                }
	            }
	        }

	        // Step 3: latest WAITING list
	        try (PreparedStatement ps = conn.prepareStatement(sqlLatestWaiting)) {
	            ps.setString(1, subscriberUsername);
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    String code = rs.getString("confirmation_code");
	                    if (code != null && !code.isBlank()) {
	                        return new ActiveCodeResult("WAITING_LIST", code.trim());
	                    }
	                }
	            }
	        }

	        return null;

	    } finally {
	        pool.releaseConnection(pc);
	    }
	}
 
 /**
  * Lists all active items for a subscriber.
  * <p>
  * Includes:
  * - ASSIGNED waiting-list entries
  * - Active reservations (CONFIRMED / PENDING / ARRIVED)
  * - WAITING waiting-list entries
  *
  * Results are ordered by urgency and time.
  *
  * @param subscriberUsername subscriber username
  * @return list of active terminal items
  * @throws Exception on database error
  */
 public static List<TerminalActiveItemDTO> listActiveItemsBySubscriberUsername(String subscriberUsername) throws Exception {

     List<TerminalActiveItemDTO> out = new ArrayList<>();

     if (subscriberUsername == null) return out;
     subscriberUsername = subscriberUsername.trim();
     if (subscriberUsername.isEmpty()) return out;

     // 1) ASSIGNED waiting list (urgent now)
     String sqlAssignedWaiting = """
         SELECT
             w.confirmation_code AS code,
             w.status AS status,
             w.request_time AS time,
             w.num_of_customers AS people
         FROM user_activity ua
         JOIN waiting_list w ON w.waiting_id = ua.waiting_id
         WHERE ua.subscriber_username = ?
           AND ua.waiting_id IS NOT NULL
           AND w.status = 'ASSIGNED'
         ORDER BY w.request_time DESC
     """;

     // 2) Closest upcoming active reservations
     String sqlClosestReservation = """
         SELECT
             r.confirmation_code AS code,
             r.status AS status,
             r.reservation_time AS time,
             r.num_of_customers AS people
         FROM user_activity ua
         JOIN reservation r ON r.reservation_id = ua.reservation_id
         WHERE ua.subscriber_username = ?
           AND ua.reservation_id IS NOT NULL
           AND r.status IN ('CONFIRMED','PENDING','ARRIVED')
         ORDER BY
           CASE WHEN r.reservation_time >= NOW() THEN 0 ELSE 1 END,
           ABS(TIMESTAMPDIFF(SECOND, r.reservation_time, NOW())) ASC
     """;

     // 3) WAITING waiting list (fallback)
     String sqlLatestWaiting = """
         SELECT
             w.confirmation_code AS code,
             w.status AS status,
             w.request_time AS time,
             w.num_of_customers AS people
         FROM user_activity ua
         JOIN waiting_list w ON w.waiting_id = ua.waiting_id
         WHERE ua.subscriber_username = ?
           AND ua.waiting_id IS NOT NULL
           AND w.status = 'WAITING'
         ORDER BY w.request_time DESC
     """;

     MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
     PooledConnection pc = pool.getConnection();
     Connection conn = pc.getConnection();

     try {

         // Step 1: ASSIGNED waiting
         try (PreparedStatement ps = conn.prepareStatement(sqlAssignedWaiting)) {
             ps.setString(1, subscriberUsername);
             try (ResultSet rs = ps.executeQuery()) {
                 while (rs.next()) {
                     String code = rs.getString("code");
                     if (code == null || code.isBlank()) continue;

                     out.add(new TerminalActiveItemDTO(
                         "WAITING",
                         code.trim(),
                         rs.getString("status"),
                         rs.getTimestamp("time"),
                         rs.getInt("people")
                     ));
                 }
             }
         }

         // Step 2: reservations
         try (PreparedStatement ps = conn.prepareStatement(sqlClosestReservation)) {
             ps.setString(1, subscriberUsername);
             try (ResultSet rs = ps.executeQuery()) {
                 while (rs.next()) {
                     String code = rs.getString("code");
                     if (code == null || code.isBlank()) continue;

                     out.add(new TerminalActiveItemDTO(
                         "RESERVATION",
                         code.trim(),
                         rs.getString("status"),
                         rs.getTimestamp("time"),
                         rs.getInt("people")
                     ));
                 }
             }
         }

         // Step 3: WAITING waiting list
         try (PreparedStatement ps = conn.prepareStatement(sqlLatestWaiting)) {
             ps.setString(1, subscriberUsername);
             try (ResultSet rs = ps.executeQuery()) {
                 while (rs.next()) {
                     String code = rs.getString("code");
                     if (code == null || code.isBlank()) continue;

                     out.add(new TerminalActiveItemDTO(
                         "WAITING",
                         code.trim(),
                         rs.getString("status"),
                         rs.getTimestamp("time"),
                         rs.getInt("people")
                     ));
                 }
             }
         }

         return out;

     } finally {
         pool.releaseConnection(pc);
     }
 }

}
