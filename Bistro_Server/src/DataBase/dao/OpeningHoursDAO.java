package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import DataBase.MySQLConnectionPool;
import DataBase.PooledConnection;
import common.dto.OpeningHoursDTO;

public class OpeningHoursDAO {

    // =============================================================
    // 1. RESTORED METHOD: Used by TxtOpeningHoursImporter
    // =============================================================
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

            if (specialDate == null) {
                ps.setNull(5, java.sql.Types.DATE);
            } else {
                ps.setDate(5, specialDate);
            }

            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // =============================================================
    // 2. NEW METHOD: Used by UI to Add Special Dates (Logic Fix)
    // =============================================================
    public static void insertSpecialHour(String dateStr, String dayOfWeekIgnored, String open, String close) throws Exception {
        
        // 1. Parse the date string (e.g., "2025-12-31")
        LocalDate date = LocalDate.parse(dateStr);
        
        // 2. Calculate the real English day name (e.g., "WEDNESDAY" -> "Wednesday")
        // This satisfies the Database Enum constraint ('Monday', 'Tuesday'...)
        String realDayName = date.getDayOfWeek().name();
        realDayName = realDayName.substring(0, 1) + realDayName.substring(1).toLowerCase();

        String sql = "INSERT INTO opening_hours (day_of_week, open_time, close_time, is_special, special_date) VALUES (?, ?, ?, 'YES', ?)";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, realDayName); 
            ps.setString(2, open);
            ps.setString(3, close);
            ps.setDate(4, Date.valueOf(date)); // Convert LocalDate to SQL Date
            
            ps.executeUpdate();
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // =============================================================
    // 3. GET ALL HOURS (UI Display)
    // =============================================================
    public static List<OpeningHoursDTO> getAllOpeningHours() throws Exception {
        List<OpeningHoursDTO> list = new ArrayList<>();
        // Sort: Regular first (is_special='NO'), then Special (is_special='YES') ordered by date
        String sql = "SELECT * FROM opening_hours ORDER BY is_special ASC, special_date ASC, hours_id ASC";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                boolean isSpecial = "YES".equalsIgnoreCase(rs.getString("is_special"));
                
                // TRICK: If it's special, show "Special" in the table column instead of "Wednesday"
                String displayDay = isSpecial ? "Special" : rs.getString("day_of_week");

                // Safely format time (substring to remove seconds if needed)
                String oTime = rs.getString("open_time");
                String cTime = rs.getString("close_time");

                list.add(new OpeningHoursDTO(
                    rs.getInt("hours_id"),
                    displayDay,
                    oTime,
                    cTime,
                    isSpecial,
                    rs.getString("special_date")
                ));
            }
        } finally {
            pool.releaseConnection(pc);
        }
        return list;
    }

    // =============================================================
    // 4. UPDATE HOUR (Edit Button)
    // =============================================================
    public static boolean updateOpeningHour(int id, String open, String close) throws Exception {
        String sql = "UPDATE opening_hours SET open_time=?, close_time=? WHERE hours_id=?";
        
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, open);
            ps.setString(2, close);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // =============================================================
    // 5. DELETE HOUR (Remove Special)
    // =============================================================
    public static boolean deleteOpeningHour(int id) throws Exception {
        String sql = "DELETE FROM opening_hours WHERE hours_id=?";
        
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } finally {
            pool.releaseConnection(pc);
        }
    }

    // =============================================================
    // 6. GET TODAY'S HOURS (Top Label)
    // =============================================================
    public static String getHoursForDate(LocalDate date) throws Exception {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            // A. Check for SPECIAL date first
            String sqlSpecial = "SELECT open_time, close_time FROM opening_hours WHERE is_special='YES' AND special_date=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSpecial)) {
                ps.setDate(1, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return formatTime(rs.getString(1)) + " - " + formatTime(rs.getString(2)) + " (Special)";
                    }
                }
            }

            // B. If no special date, check REGULAR day of week
            String dayName = date.getDayOfWeek().toString(); 
            dayName = dayName.substring(0, 1) + dayName.substring(1).toLowerCase(); // "MONDAY" -> "Monday"

            String sqlRegular = "SELECT open_time, close_time FROM opening_hours WHERE day_of_week=? AND is_special='NO' LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlRegular)) {
                ps.setString(1, dayName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return formatTime(rs.getString(1)) + " - " + formatTime(rs.getString(2));
                    }
                }
            }
            
            return "Closed";
            
        } finally {
            pool.releaseConnection(pc);
        }
    }
    
    // Helper to keep times clean (09:00 instead of 09:00:00)
    private static String formatTime(String t) {
        if (t == null) return "";
        return (t.length() > 5) ? t.substring(0, 5) : t;
    }
}