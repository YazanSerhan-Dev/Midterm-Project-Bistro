package DataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    /**
     * מחזיר את כל ההזמנות מטבלת `order`.
     */
    public List<Reservation> getAllReservations() throws SQLException {
        String sql =
                "SELECT order_number, " +
                "       order_date, " +
                "       number_of_guests, " +
                "       confirmation_code, " +
                "       subscriber_id, " +
                "       date_of_placing_order " +
                "FROM `order`";

        List<Reservation> list = new ArrayList<>();

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // ---- קבלת חיבור מה-pool ----
            conn = pool.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                int orderNumber        = rs.getInt("order_number");
                Date orderDate         = rs.getDate("order_date");
                int numberOfGuests     = rs.getInt("number_of_guests");
                int confirmationCode   = rs.getInt("confirmation_code");
                int subscriberId       = rs.getInt("subscriber_id");
                Date dateOfPlacingOrder= rs.getDate("date_of_placing_order");

                Reservation r = new Reservation(
                        orderNumber,
                        orderDate,
                        numberOfGuests,
                        confirmationCode,
                        subscriberId,
                        dateOfPlacingOrder
                );

                list.add(r);
            }

        } finally {
            // ---- לסגור אובייקטים של JDBC ----
            if (rs != null) {
                try { rs.close(); } catch (SQLException ignored) {}
            }
            if (ps != null) {
                try { ps.close(); } catch (SQLException ignored) {}
            }
            // ---- להחזיר את החיבור ל-pool (לא conn.close!) ----
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }

        return list;
    }

    /**
     * עדכון תאריך ומספר אורחים להזמנה קיימת.
     */
    public void updateReservation(int reservationNumber,
                                  Date newDate,
                                  int newGuests) throws SQLException {

        String sql =
                "UPDATE `order` " +
                "SET order_date = ?, number_of_guests = ? " +
                "WHERE order_number = ?";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = pool.getConnection();
            ps = conn.prepareStatement(sql);

            ps.setDate(1, newDate);
            ps.setInt(2, newGuests);
            ps.setInt(3, reservationNumber);

            ps.executeUpdate();

        } finally {
            if (ps != null) {
                try { ps.close(); } catch (SQLException ignored) {}
            }
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }
    }
}






