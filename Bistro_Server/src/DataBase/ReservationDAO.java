package DataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Reservation table.
 * Uses MySQLConnectionPool + PooledConnection instead of opening new JDBC connections each time.
 */
public class ReservationDAO {

    /**
     * Loads all reservations from the `order` table using a pooled connection.
     */
    public List<Reservation> getAllReservations() throws SQLException {
        String sql = "SELECT order_number, order_date, number_of_guests, " +
                     "confirmation_code, subscriber_id, date_of_placing_order " +
                     "FROM `order`";

        List<Reservation> list = new ArrayList<>();

        // get pooled connection (not DriverManager.getConnection)
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn   = pool.getConnection();
        Connection conn          = pConn.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int num     = rs.getInt("order_number");
                Date date   = rs.getDate("order_date");
                int guests  = rs.getInt("number_of_guests");
                int conf    = rs.getInt("confirmation_code");
                int subId   = rs.getInt("subscriber_id");
                Date placed = rs.getDate("date_of_placing_order");

                list.add(new Reservation(num, date, guests, conf, subId, placed));
            }
        } finally {
            // IMPORTANT: return to pool (do NOT close conn directly)
            pool.releaseConnection(pConn);
        }

        return list;
    }

    /**
     * Updates order_date and number_of_guests for a given reservation (by order_number).
     */
    public void updateReservation(int reservationNumber,
                                  Date newDate,
                                  int newGuests) throws SQLException {

        String sql = "UPDATE `order` " +
                     "SET order_date = ?, number_of_guests = ? " +
                     "WHERE order_number = ?";

        // get pooled connection
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn   = pool.getConnection();
        Connection conn          = pConn.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, newDate);
            ps.setInt(2, newGuests);
            ps.setInt(3, reservationNumber);
            ps.executeUpdate();
        } finally {
            // return to pool instead of closing the physical connection
            pool.releaseConnection(pConn);
        }
    }
}








