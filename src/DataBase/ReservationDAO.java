package DataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public List<Reservation> getAllReservations() throws SQLException {
        String sql = "SELECT order_number, order_date, number_of_guests, " +
                     "confirmation_code, subscriber_id, date_of_placing_order " +
                     "FROM `order`";

        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int num   = rs.getInt("order_number");
                Date date = rs.getDate("order_date");
                int guests = rs.getInt("number_of_guests");
                int conf   = rs.getInt("confirmation_code");
                int subId  = rs.getInt("subscriber_id");
                Date placed = rs.getDate("date_of_placing_order");

                list.add(new Reservation(num, date, guests, conf, subId, placed));
            }
        }
        return list;
    }

    // עדכון order_date ו number_of_guests לפי order_number
    public void updateReservation(int reservationNumber,
                                  Date newDate,
                                  int newGuests) throws SQLException {

        String sql = "UPDATE `order` " +
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



