package Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    // שליפת כל ההזמנות מה-DB
    public List<Reservation> getAllReservations() throws SQLException {
        List<Reservation> list = new ArrayList<>();

        String sql =
                "SELECT order_number, order_date, number_of_guests, " +
                "confirmation_code, subscriber_id, date_of_placing_order " +
                "FROM `order`";   // שים לב ל־` בגלל שהמילה order היא מילה שמורה

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Reservation r = new Reservation(
                        rs.getInt("order_number"),
                        rs.getDate("order_date"),
                        rs.getInt("number_of_guests"),
                        rs.getInt("confirmation_code"),
                        rs.getInt("subscriber_id"),
                        rs.getDate("date_of_placing_order")
                );
                list.add(r);
            }
        }
        return list;
    }

    // עדכון order_date ו-number_of_guests להזמנה קיימת
    public void updateReservation(int reservationNumber, Date newDate, int newGuests) throws SQLException {

        String sql =
                "UPDATE `order` " +
                "SET order_date = ?, number_of_guests = ? " +
                "WHERE order_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, newDate);
            ps.setInt(2, newGuests);
            ps.setInt(3, reservationNumber);

            ps.executeUpdate();
        }
    }
}


