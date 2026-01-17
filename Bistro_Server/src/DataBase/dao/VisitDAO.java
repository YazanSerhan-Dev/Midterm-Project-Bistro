package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.CurrentDinersDTO;
/**
 * Data Access Object for visit records.
 * <p>
 * Handles visit lifecycle operations such as creation,
 * existence checks (for reservations or waiting list),
 * and retrieval of active diners for UI display.
 */
public class VisitDAO {

    // =============================================================
    // 1. INSERT METHODS (Common)
    // =============================================================
	/**
	 * Inserts a visit record with explicit start and end times.
	 * <p>
	 * Used mainly for completed or historical visits.
	 *
	 * @param activityId linked user_activity ID
	 * @param tableId table identifier
	 * @param start actual start time
	 * @param end actual end time
	 * @throws Exception on database error
	 */
    public static void insertVisit(
            int activityId, String tableId,
            Timestamp start, Timestamp end) throws Exception {

        String sql = """
            INSERT INTO visit
            (activity_id, table_id, actual_start_time, actual_end_time)
            VALUES (?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setString(2, tableId);
            ps.setTimestamp(3, start);
            ps.setTimestamp(4, end);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    /**
     * Inserts a visit using an existing connection.
     * <p>
     * Automatically sets start time to NOW() and leaves end time NULL.
     * Intended for transactional check-in flows.
     *
     * @param conn active database connection
     * @param activityId linked user_activity ID
     * @param tableId table identifier
     * @throws Exception on database error
     */
    public static void insertVisit(Connection conn, int activityId, String tableId) throws Exception {
        String sql = """
            INSERT INTO visit (activity_id, table_id, actual_start_time, actual_end_time)
            VALUES (?, ?, NOW(), NULL)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            ps.setString(2, tableId);
            ps.executeUpdate();
        }
    }

    // =============================================================
    // 2. CHECK EXISTENCE METHODS (From MAIN Branch)
    // =============================================================
    /**
     * Checks whether a visit already exists for a waiting-list entry.
     *
     * @param waitingId waiting-list ID
     * @return true if a visit exists, false otherwise
     * @throws Exception on database error
     */
    public static boolean existsVisitForWaitingId(int waitingId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.waiting_id = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeQuery().next();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    /**
     * Same as existsVisitForWaitingId, using an existing connection.
     * <p>
     * Used inside transactional flows.
     *
     * @param conn active database connection
     * @param waitingId waiting-list ID
     * @return true if a visit exists
     * @throws Exception on database error
     */
    public static boolean existsVisitForWaitingId(Connection conn, int waitingId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.waiting_id = ?
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeQuery().next();
        }
    }
    /**
     * Checks whether a visit already exists for a reservation.
     *
     * @param reservationId reservation ID
     * @return true if a visit exists, false otherwise
     * @throws Exception on database error
     */
    public static boolean existsVisitForReservationId(int reservationId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.reservation_id = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            return ps.executeQuery().next();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    /**
     * Same as existsVisitForReservationId, using an existing connection.
     *
     * @param conn active database connection
     * @param reservationId reservation ID
     * @return true if a visit exists
     * @throws Exception on database error
     */
    public static boolean existsVisitForReservationId(Connection conn, int reservationId) throws Exception {
        String sql = """
            SELECT 1
            FROM visit v
            JOIN user_activity ua ON ua.activity_id = v.activity_id
            WHERE ua.reservation_id = ?
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            return ps.executeQuery().next();
        }
    }

    // =============================================================
    // 3. UI DISPLAY METHODS (From HEAD/Your Branch)
    // =============================================================
    /**
     * Retrieves all currently active diners in the restaurant.
     * <p>
     * Includes both reservation-based visits and waiting-list walk-ins.
     * Used for real-time staff UI display.
     *
     * @return list of active diners with table number, name, party size and start time
     */
    public static List<CurrentDinersDTO> getActiveDiners() {
        List<CurrentDinersDTO> list = new ArrayList<>();
        
        // Updated SQL: Joins waiting_list to get the count for walk-ins
        String sql = """
            SELECT 
                v.table_id, 
                v.actual_start_time, 
                COALESCE(r.num_of_customers, w.num_of_customers) AS final_count, 
                s.name AS subscriber_name,
                ua.guest_email
            FROM visit v
            JOIN user_activity ua ON v.activity_id = ua.activity_id
            LEFT JOIN reservation r ON ua.reservation_id = r.reservation_id
            LEFT JOIN waiting_list w ON ua.waiting_id = w.waiting_id
            LEFT JOIN subscribers s ON ua.subscriber_username = s.username
            WHERE v.actual_end_time IS NULL OR v.actual_end_time > NOW()
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = null;

        try {
            pc = pool.getConnection();
            Connection conn = pc.getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int tableNum = 0;
                    String tIdStr = rs.getString("table_id"); 
                    try {
                        tableNum = Integer.parseInt(tIdStr.replaceAll("\\D", ""));
                    } catch (Exception e) { tableNum = 0; }

                    String subName = rs.getString("subscriber_name");
                    String gstEmail = rs.getString("guest_email");
                    
                    String displayName = "Guest";
                    if (subName != null && !subName.isBlank()) {
                        displayName = subName; 
                    } else if (gstEmail != null && !gstEmail.isBlank()) {
                        displayName = gstEmail;
                    }

                    // Now gets the correct count for both Reservations and Waiting List
                    int count = rs.getInt("final_count");
                    
                    Timestamp ts = rs.getTimestamp("actual_start_time");
                    String timeStr = (ts != null) ? ts.toLocalDateTime().toLocalTime().toString() : "-";

                    list.add(new CurrentDinersDTO(tableNum, displayName, count, timeStr, "Seated"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in VisitDAO.getActiveDiners: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (pc != null) pool.releaseConnection(pc);
        }
        return list;
    }
    /**
     * Retrieves visit history summaries for a subscriber.
     * <p>
     * Each entry contains a simple textual summary with table ID and visit time.
     *
     * @param username subscriber username
     * @return list of visit summary strings
     */
    public static List<String> getVisitsBySubscriber(String username) {
        List<String> list = new ArrayList<>();
        // ✅ JOIN with user_activity to find the username
        String sql = "SELECT v.* FROM visit v " +
                     "JOIN user_activity ua ON v.activity_id = ua.activity_id " +
                     "WHERE ua.subscriber_username = ?";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String start = rs.getString("actual_start_time");
                    String end = rs.getString("actual_end_time");
                    String table = rs.getString("table_id");
                    
                    // Simple summary string
                    list.add("Table: " + table + " | Date: " + start);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }
}