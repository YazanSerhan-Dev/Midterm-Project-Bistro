package DataBase.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
    
    public static boolean isOpenForReservation(Timestamp startTs, int diningMinutes) throws Exception {
        if (startTs == null) return false;

        LocalDateTime start = startTs.toLocalDateTime();
        LocalDate date = start.toLocalDate();
        LocalTime time = start.toLocalTime();

        // compute end time (2 hours later usually)
        LocalDateTime end = start.plusMinutes(diningMinutes);
        LocalDate endDate = end.toLocalDate();
        LocalTime endTime = end.toLocalTime();

        // We only support dining window within same calendar day in this simple logic
        // (If you allow crossing midnight, tell me and I’ll upgrade it.)
        if (!endDate.equals(date)) return false;

        // 1) get today's opening interval (special date overrides weekly)
        OpenInterval interval = getOpenIntervalForDate(date);
        if (interval == null) return false;

        // must be within [open, close]
        // start >= open AND end <= close
        return !time.isBefore(interval.open) && !endTime.isAfter(interval.close);
    }

    /** Small helper struct */
    private static class OpenInterval {
        final LocalTime open;
        final LocalTime close;
        OpenInterval(LocalTime open, LocalTime close) {
            this.open = open;
            this.close = close;
        }
    }

    private static OpenInterval getOpenIntervalForDate(LocalDate date) throws Exception {

        // special date wins (is_special='YES')
        String sqlSpecial = """
            SELECT open_time, close_time
            FROM opening_hours
            WHERE is_special = 'YES'
              AND special_date = ?
            LIMIT 1
        """;

        // otherwise use day_of_week
        String sqlWeekly = """
            SELECT open_time, close_time
            FROM opening_hours
            WHERE (is_special IS NULL OR is_special = 'NO')
              AND day_of_week = ?
            LIMIT 1
        """;

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pc = pool.getConnection();
        Connection conn = pc.getConnection();

        try {
            // special
            try (PreparedStatement ps = conn.prepareStatement(sqlSpecial)) {
                ps.setDate(1, java.sql.Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Time o = rs.getTime("open_time");
                        Time c = rs.getTime("close_time");
                        if (o == null || c == null) return null;
                        return new OpenInterval(o.toLocalTime(), c.toLocalTime());
                    }
                }
            }

            // weekly
            DayOfWeek dow = date.getDayOfWeek();
            String dowStr = date.getDayOfWeek()
                    .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH); // "Sunday"

            try (PreparedStatement ps = conn.prepareStatement(sqlWeekly)) {
                ps.setString(1, dowStr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Time o = rs.getTime("open_time");
                        Time c = rs.getTime("close_time");
                        if (o == null || c == null) return null;
                        return new OpenInterval(o.toLocalTime(), c.toLocalTime());
                    }
                }
            }

            return null;

        } finally {
            pool.releaseConnection(pc);
        }
    }

    public static List<String> getAvailableTimeSlots(LocalDate date, int slotMinutes, int diningMinutes) throws Exception {
        List<String> out = new ArrayList<>();
        if (date == null) return out;

        // Use same opening-hours source you already implemented
        OpenInterval interval = getOpenIntervalForDate(date);
        if (interval == null) return out; // closed / not configured

        LocalTime open = interval.open;
        LocalTime close = interval.close;

        // last start time must allow the full dining window
        LocalTime lastStart = close.minusMinutes(diningMinutes);
        if (lastStart.isBefore(open)) return out;

        LocalDate today = LocalDate.now();

     // block dates more than 1 month ahead (no slots)
     if (date.isAfter(today.plusMonths(1))) {
         return out; // empty list
     }

     LocalTime minStart = open;

     if (date.equals(today)) {
         // ✅ 1 hour from now (truncate seconds/nanos)
         LocalTime nowPlus1h = java.time.LocalTime.now()
                 .withSecond(0).withNano(0)
                 .plusHours(1);

         if (nowPlus1h.isAfter(minStart)) {
             minStart = nowPlus1h;
         }
     }

     // also truncate seconds/nanos always
     minStart = minStart.withSecond(0).withNano(0);

     // round to slot boundary (30 mins)
     int mod = minStart.getMinute() % slotMinutes;
     if (mod != 0) {
         minStart = minStart.plusMinutes(slotMinutes - mod);
     }

        // Generate slots
        for (LocalTime t = minStart; !t.isAfter(lastStart); t = t.plusMinutes(slotMinutes)) {
            out.add(String.format("%02d:%02d", t.getHour(), t.getMinute()));
        }

        return out;
    }

}
