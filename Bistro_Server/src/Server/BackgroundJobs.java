package Server;

import DataBase.dao.BillDAO;
import DataBase.dao.ReservationDAO;
import DataBase.dao.RestaurantTableDAO;
import DataBase.dao.WaitingListDAO;
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
     // Thread #2: Waiting-list maintenance ONLY (NO auto-expire/free tables)
     // =========================
     scheduler.scheduleAtFixedRate(() -> {
         try {
             // ✅ Keep waiting list maintenance
             int canceled = WaitingListDAO.cancelAssignedOver15Minutes();
             if (canceled > 0) {
                 System.out.println("[JOB] CANCELED assigned waiting over 15min: " + canceled);
             }

             int oldCanceled = WaitingListDAO.cancelWaitingOlderThanHours(4);
             if (oldCanceled > 0) {
                 System.out.println("[JOB] CANCELED old WAITING entries (4h): " + oldCanceled);
             }

             // ✅ Assign WAITING -> ASSIGNED + notify (your existing logic)
             int assignedCount = 0;

             while (true) {
                 var next = WaitingListDAO.getOldestWaitingThatFits();
                 if (next == null) break;

                 boolean ok = WaitingListDAO.markAssignedById(next.getId());
                 if (!ok) break;

                 assignedCount++;

                 String email = WaitingListDAO.getGuestEmailForWaitingId(next.getId());
                 if (email != null && !email.isBlank()) {
                     EmailService.sendWaitingTableReady(email, next.getConfirmationCode());
                     System.out.println("[JOB] Waiting ASSIGNED email sent to: " + email + " | Code: " + next.getConfirmationCode());
                 } else {
                     System.out.println("[JOB] Waiting ASSIGNED but email not found | Code: " + next.getConfirmationCode());
                 }
             }

             if (assignedCount > 0) {
                 System.out.println("[JOB] ASSIGNED waiting entries: " + assignedCount);
             }

         } catch (Exception e) {
             System.out.println("[JOB] waiting maintenance error: " + e.getMessage());
         }
     }, 10, 30, TimeUnit.SECONDS);


        // =========================
        // Thread #3: 2-hour reminder (EMAIL + SMS)
        // =========================
        scheduler.scheduleAtFixedRate(() -> {
            try {
                runTwoHourReminderJob();
             // 1) try reserve for pending reservations (priority)
                ReservationDAO.autoReserveForPendingReservations();

                // 2) release tables if customer didn't confirm in time
                ReservationDAO.releaseExpiredAutoReserved(15);
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
     // =========================
     // Thread: Bill reminder after 2 hours (visit-based)
     // =========================
     scheduler.scheduleAtFixedRate(() -> {
         try {
             List<BillDAO.BillReminderRow> due = BillDAO.findBillsNeedingReminder(25);

             for (BillDAO.BillReminderRow row : due) {
                 try {
                     EmailService.sendBillReminder(row.email, row.confirmationCode);
                     BillDAO.markReminderSent(row.billId);
                     System.out.println("[JOB] Bill reminder sent to " + row.email + " | code=" + row.confirmationCode);
                 } catch (Exception sendErr) {
                     System.out.println("[JOB] Bill reminder failed for billId=" + row.billId + " : " + sendErr.getMessage());
                     // Do NOT mark sent if email failed
                 }
             }
         } catch (Exception e) {
             System.out.println("[JOB] Bill reminder job error: " + e.getMessage());
         }
     }, 30, 300, TimeUnit.SECONDS); // start after 30s, then every 300s (5 min)

    }
    
}


