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
    private static final Object TABLE_ASSIGN_LOCK = new Object();

    // ✅ In-memory protection: reminder sent once per server run (reservation reminder only)
    private static final Set<Integer> reminderSent = ConcurrentHashMap.newKeySet();

    public static void start() {

        // ✅ prevents duplicates
        if (!started.compareAndSet(false, true)) {
            System.out.println("[JOB] BackgroundJobs already running.");
            return;
        }

        // We run 4 periodic tasks => pool size 4
        scheduler = Executors.newScheduledThreadPool(4);

        // =========================
        // Thread #1: Cancel NO-SHOWS (15 minutes)
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
     // Thread #2 (combined): Pending reservations FIRST, then waiting list
     // =========================
     scheduler.scheduleAtFixedRate(() -> {
         synchronized (TABLE_ASSIGN_LOCK) {
             try {
            	 

                 // (0) ✅ Release expired RESERVED holds for waiting list FIRST
                 int released = RestaurantTableDAO.releaseExpiredReservedTablesForWaiting();
                 if (released > 0) {
                     System.out.println("[JOB] Released expired RESERVED tables for waiting: " + released);
                 }

                 // (1) Waiting-list maintenance (also frees reserved tables by canceling ASSIGNED no-show)
                 int canceled = WaitingListDAO.cancelAssignedOver15Minutes();
                 if (canceled > 0) {
                     System.out.println("[JOB] CANCELED assigned waiting over 15min: " + canceled);
                 }

                 int oldCanceled = WaitingListDAO.cancelWaitingOlderThanHours(4);
                 if (oldCanceled > 0) {
                     System.out.println("[JOB] CANCELED old WAITING entries (4h): " + oldCanceled);
                 }

                 // (2) ✅ Reservations have priority (pending reserve)
                 ReservationDAO.autoReserveForPendingReservations();

                 int canceledPending = ReservationDAO.cancelPendingReservationsWithExpiredHold();
                 if (canceledPending > 0) {
                     System.out.println("[JOB] CANCELED pending reservations after reserved timeout: " + canceledPending);
                 }

                 // (3) Assign waiting list (after reservations had their chance)
                 int assignedCount = 0;

                 while (true) {
                     var next = WaitingListDAO.assignNextWaitingByReservingTable();
                     if (next == null) break;

                     assignedCount++;

                     String email = WaitingListDAO.getGuestEmailForWaitingId(next.getId());
                     String code = next.getConfirmationCode();

                     if (email != null && !email.isBlank() && code != null && !code.isBlank()) {
                         EmailService.sendWaitingTableReady(email, code);
                         System.out.println("[JOB] Waiting ASSIGNED email sent to: " + email + " | Code: " + code);
                     } else {
                         System.out.println("[JOB] Waiting ASSIGNED but email/code missing | id=" + next.getId());
                     }
                 }

                 if (assignedCount > 0) {
                     System.out.println("[JOB] ASSIGNED waiting entries: " + assignedCount);
                 }

             } catch (Exception e) {
                 System.out.println("[JOB] assignment cycle error: " + e.getMessage());
             }
         }
     }, 10, 20, TimeUnit.SECONDS);

        // =========================
        // Thread #4: Bill reminder after 2 hours (visit-based)
        // =========================
        scheduler.scheduleAtFixedRate(() -> {
            try {
                runBillReminderJobOnce();
            } catch (Exception e) {
                System.out.println("[JOB] Bill reminder job error: " + e.getMessage());
            }
        }, 30, 300, TimeUnit.SECONDS); // start after 30s, then every 5 min

        // =========================
        // OPTIONAL Thread #5: Reservation reminder (upcoming reservation in ~2 hours)
        // If you don't need it, you can delete this thread + method.
        // =========================
        scheduler.scheduleAtFixedRate(() -> {
            try {
                runUpcomingReservationReminderOnce();
            } catch (Exception e) {
                System.out.println("[JOB] reservation reminder error: " + e.getMessage());
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
    // Bill reminder logic (visit-based) - runs ONCE each time thread triggers
    // =========================================================
    private static void runBillReminderJobOnce() throws Exception {

        List<BillDAO.BillReminderRow> due = BillDAO.findBillsNeedingReminder(25);

        for (BillDAO.BillReminderRow row : due) {
            try {
                EmailService.sendBillReminder(row.email, row.confirmationCode);
                BillDAO.markReminderSent(row.billId);

                System.out.println("[JOB] Bill reminder sent to " + row.email +
                        " | code=" + row.confirmationCode);

            } catch (Exception sendErr) {
                System.out.println("[JOB] Bill reminder failed for billId=" + row.billId +
                        " : " + sendErr.getMessage());
                // Do NOT mark sent if email failed
            }
        }
    }

    // =========================================================
    // OPTIONAL: Upcoming reservation reminder (~2 hours before reservation_time)
    // This is NOT the bill reminder. If you don't want it, remove it.
    // =========================================================
    private static void runUpcomingReservationReminderOnce() throws Exception {

        List<Reservation> all = new ReservationDAO().getAllReservations();

        LocalDateTime now = LocalDateTime.now();

        // window around "reservation is in 2 hours"
        LocalDateTime from = now.plusHours(2).minusMinutes(2);
        LocalDateTime to = now.plusHours(2).plusMinutes(2);

        for (Reservation r : all) {

            // already reminded (in this server run)
            if (reminderSent.contains(r.getReservationId())) continue;

            if (!"CONFIRMED".equalsIgnoreCase(r.getStatus())) continue;

            Timestamp ts = r.getReservationTime();
            if (ts == null) continue;

            LocalDateTime resTime = ts.toLocalDateTime();

            if (resTime.isBefore(from) || resTime.isAfter(to)) continue;

            try {
                String email = ReservationDAO.getReservationEmail(r.getReservationId());
                if (email != null && !email.isBlank()) {

                    String timeStr = resTime.toString().replace('T', ' ');

                    EmailService.sendReservationReminder(
                            email,
                            r.getConfirmationCode(),
                            timeStr
                    );

                    System.out.println("[REMINDER] Reservation reminder email sent to " + email +
                            " for reservation " + r.getReservationId());
                }

                // SMS stub (optional)
                String phone = ReservationDAO.getReservationPhone(r.getReservationId());
                if (phone != null && !phone.isBlank()) {
                    System.out.println("[SMS] Reminder to " + phone +
                            " | Code: " + r.getConfirmationCode());
                }

                reminderSent.add(r.getReservationId());

            } catch (Exception e) {
                System.out.println("[REMINDER] Failed for reservation " +
                        r.getReservationId() + ": " + e.getMessage());
            }
        }
    }
}


