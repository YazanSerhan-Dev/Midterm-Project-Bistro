package Server;

import DataBase.dao.ReservationDAO;
import DataBase.dao.RestaurantTableDAO;
import DataBase.Reservation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundJobs {

    private static ScheduledExecutorService scheduler;
    private static final AtomicBoolean started = new AtomicBoolean(false);

    // ✅ In-memory protection: reminder sent once per server run
    private static final Set<Integer> reminderSent =
            ConcurrentHashMap.newKeySet();

    public static void start() {
        // ✅ prevents duplicates
        if (!started.compareAndSet(false, true)) {
            System.out.println("[JOB] BackgroundJobs already running.");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(3);

        // =========================
        // Thread #1: Cancel NO-SHOWS
        // =========================
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int updated = ReservationDAO.cancelNoShows15Min();
                if (updated > 0) {
                    System.out.println("[JOB] CANCELED no-shows (15 min): " + updated);
                }
            } catch (Exception e) {
                System.out.println("[JOB] cancelNoShows15Min error: " + e.getMessage());
            }
        }, 5, 30, TimeUnit.SECONDS);

        // =========================
        // Thread #2: Expire FINISHED reservations + free tables
        // =========================
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int expired = ReservationDAO.expireFinishedArrived2Hours();
                if (expired > 0) {
                    System.out.println("[JOB] EXPIRED finished ARRIVED (2 hours): " + expired);
                }

                int freed = RestaurantTableDAO.freeTablesForExpiredReservations();
                if (freed > 0) {
                    System.out.println("[JOB] Freed tables for EXPIRED reservations: " + freed);
                }

            } catch (Exception e) {
                System.out.println("[JOB] expire/free error: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);

        // =========================
        // Thread #3: 2-hour reminder (EMAIL + SMS)
        // =========================
        scheduler.scheduleAtFixedRate(() -> {
            try {
                runTwoHourReminderJob();
            } catch (Exception e) {
                System.out.println("[JOB] reminder error: " + e.getMessage());
            }
        }, 15, 60, TimeUnit.SECONDS);

        System.out.println("[JOB] BackgroundJobs started.");
    }

    public static void stop() {
        if (!started.compareAndSet(true, false)) {
            System.out.println("[JOB] BackgroundJobs not running.");
            return;
        }

        try {
            if (scheduler != null) scheduler.shutdownNow();
        } finally {
            scheduler = null;
        }

        System.out.println("[JOB] BackgroundJobs stopped.");
    }

    // =========================================================
    // 2-hour reminder logic (NO DB CHANGE)
    // =========================================================
    private static void runTwoHourReminderJob() throws Exception {

        List<Reservation> all = new ReservationDAO().getAllReservations();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusHours(2).minusMinutes(2);
        LocalDateTime to   = now.plusHours(2).plusMinutes(2);
        
        for (Reservation r : all) {

            // already reminded (in this server run)
            if (reminderSent.contains(r.getReservationId())) continue;

            if (!"CONFIRMED".equalsIgnoreCase(r.getStatus())) continue;

            Timestamp ts = r.getReservationTime();
            if (ts == null) continue;

            LocalDateTime resTime = ts.toLocalDateTime();

            if (resTime.isBefore(from) || resTime.isAfter(to)) continue;

            // =========================
            // SEND REMINDER
            // =========================
            try {
                String email = ReservationDAO.getReservationEmail(r.getReservationId());
                if (email != null && !email.isBlank()) {

                    String timeStr = resTime.toString().replace('T', ' ');

                    EmailService.sendReservationReminder(
                            email,
                            r.getConfirmationCode(),
                            timeStr
                    );

                    System.out.println("[REMINDER] Email sent to " + email +
                            " for reservation " + r.getReservationId());
                }

                // SMS stub
                String phone = ReservationDAO.getReservationPhone(r.getReservationId());
                if (phone != null && !phone.isBlank()) {
                    System.out.println("[SMS] Reminder to " + phone +
                            " | Code: " + r.getConfirmationCode());
                }

                // mark as sent (in memory)
                reminderSent.add(r.getReservationId());

            } catch (Exception e) {
                System.out.println("[REMINDER] Failed for reservation "
                        + r.getReservationId() + ": " + e.getMessage());
            }
        }
    }
}


