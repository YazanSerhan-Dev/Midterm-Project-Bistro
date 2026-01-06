package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
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

    // ✅ NEW: fetch waiting list entry by its code
    public static WaitingListDTO getByCode(String code) throws Exception {
        String sql = """
            SELECT waiting_id, num_of_customers, request_time, status, confirmation_code
            FROM waiting_list
            WHERE confirmation_code = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                WaitingListDTO dto = new WaitingListDTO();
                dto.setId(rs.getInt("waiting_id"));
                dto.setPeopleCount(rs.getInt("num_of_customers"));
                dto.setStatus(rs.getString("status"));
                dto.setConfirmationCode(rs.getString("confirmation_code"));

                // note: DB table doesn’t store email/phone per your schema, so we don’t fill them here.
                return dto;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // ✅ NEW: update waiting status by code (WAITING / ASSIGNED / CANCELED)
    public static boolean updateStatusByCode(String code, String newStatus) throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = ?
            WHERE confirmation_code = ?
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, code);
            return ps.executeUpdate() > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static common.dto.WaitingListDTO getOldestWaitingThatFits() throws Exception {
        String sql = """
            SELECT w.waiting_id, w.num_of_customers, w.status, w.confirmation_code
            FROM waiting_list w
            WHERE w.status = 'WAITING'
              AND EXISTS (
                  SELECT 1
                  FROM restaurant_table t
                  WHERE t.status = 'FREE'
                    AND t.num_of_seats >= w.num_of_customers
              )
            ORDER BY w.request_time ASC
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return null;

            common.dto.WaitingListDTO dto = new common.dto.WaitingListDTO();
            dto.setId(rs.getInt("waiting_id"));
            dto.setPeopleCount(rs.getInt("num_of_customers"));
            dto.setStatus(rs.getString("status"));
            dto.setConfirmationCode(rs.getString("confirmation_code"));
            return dto;

        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    public static boolean markAssignedById(int waitingId) throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = 'ASSIGNED',
                request_time = NOW()
            WHERE waiting_id = ?
              AND status = 'WAITING'
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            return ps.executeUpdate() == 1;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int cancelAssignedOver15Minutes() throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = 'CANCELED'
            WHERE status = 'ASSIGNED'
              AND request_time < (NOW() - INTERVAL 15 MINUTE)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static String getGuestEmailForWaitingId(int waitingId) throws Exception {
        String sql = """
            SELECT guest_email
            FROM user_activity
            WHERE waiting_id = ?
            ORDER BY activity_id DESC
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, waitingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int insertWaitingReturnId(
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

        try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, numOfCustomers);
            ps.setTimestamp(2, requestTime);
            ps.setString(3, status);
            ps.setString(4, confirmationCode);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static int cancelWaitingOlderThanHours(int hours) throws Exception {
        String sql = """
            UPDATE waiting_list
            SET status = 'CANCELED'
            WHERE status = 'WAITING'
              AND request_time < (NOW() - INTERVAL ? HOUR)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, hours);
            return ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static boolean cancelIfWaitingByCode(String code) {

        String sql = """
            UPDATE waiting_list
            SET status = 'CANCELED'
            WHERE confirmation_code = ?
              AND status = 'WAITING'
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            return ps.executeUpdate() == 1;   // true only if 1 row updated
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            pool.releaseConnection(pc);
        }
    }
}




