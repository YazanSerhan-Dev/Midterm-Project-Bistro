package DataBase.dao;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.SubscriberDTO;
import common.dto.WaitingListDTO;


public class WaitingListDAO {

    public static void insertWaiting(
            int numOfCustomers,
            Timestamp requestTime,
            String status,
            String confirmationCode) throws Exception {

    	String sql = """
                INSERT INTO waiting_list
                (num_of_customers, request_time, status, confirmation_code)
                VALUES (?, ?, ?, ?)
            """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numOfCustomers);
            ps.setTimestamp(2, requestTime);
            ps.setString(3, status);
            ps.setString(4, confirmationCode);
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static List<WaitingListDTO> getAllWaitingList() {
        List<WaitingListDTO> list = new ArrayList<>();

        String sql = "SELECT waiting_id, num_of_customers, request_time, status, confirmation_code FROM waiting_list WHERE status = 'WAITING' ORDER BY request_time ASC";

        try {
            MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
            PooledConnection pc = pool.getConnection();
            Connection conn = pc.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("waiting_id");
                int count = rs.getInt("num_of_customers");
                String status = rs.getString("status");
                String code = rs.getString("confirmation_code");
                
                java.sql.Timestamp ts = rs.getTimestamp("request_time");
                String timeStr = (ts != null) ? ts.toString() : "";


                list.add(new WaitingListDTO(id, count, timeStr, status, code));
            }

            pool.releaseConnection(pc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
