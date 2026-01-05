package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Date;
import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import common.dto.SubscriberDTO;

public class SubscriberDAO {

    public static void insertSubscriber(
            String username, String password, String name,
            String phone, String email,
            String memberCode, String barcode, Date birthDate) throws Exception {

        String sql = """
            INSERT INTO subscribers
            (username, password, name, phone, email, member_code, barcode_data, birth_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            ps.setString(4, phone);
            ps.setString(5, email);
            ps.setString(6, memberCode);
            ps.setString(7, barcode);
            ps.setDate(8, birthDate);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    
    public static List<SubscriberDTO> getAllSubscribers() throws Exception {
        List<SubscriberDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM subscribers"; // Adjust table name if needed

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Assuming your table has these columns. 
                // Note: If you don't have an auto-increment ID, you might mock it or use row number.
                int id = 0; // or rs.getInt("id") if you have one
                String name = rs.getString("name"); 
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                
                // If you don't have a status column yet, default to "Active"
                String status = "Active"; 

                list.add(new SubscriberDTO(id, name, phone, email, status));
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    
}
}
