package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
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


}
