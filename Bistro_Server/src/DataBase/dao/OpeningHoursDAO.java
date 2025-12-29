package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.sql.Date;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;

public class OpeningHoursDAO {

    public static void insertOpeningHours(
            String dayOfWeek,
            Time openTime,
            Time closeTime,
            String isSpecial,
            Date specialDate) throws Exception {

        String sql = """
            INSERT INTO opening_hours
            (day_of_week, open_time, close_time, is_special, special_date)
            VALUES (?, ?, ?, ?, ?)
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dayOfWeek);
            ps.setTime(2, openTime);
            ps.setTime(3, closeTime);
            ps.setString(4, isSpecial);

            if (specialDate == null)
                ps.setNull(5, java.sql.Types.DATE);
            else
                ps.setDate(5, specialDate);

            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }
}
