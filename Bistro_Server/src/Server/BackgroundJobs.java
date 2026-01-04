package Server;

import DataBase.dao.ReservationDAO;
import DataBase.dao.RestaurantTableDAO;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundJobs {

    private static ScheduledExecutorService scheduler;
    private static final AtomicBoolean started = new AtomicBoolean(false);

    public static void start() {
        // ✅ prevents duplicates
        if (!started.compareAndSet(false, true)) {
            System.out.println("[JOB] BackgroundJobs already running.");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(2); // ✅ 2 background threads

        // Thread #1: Cancel NO-SHOWS:
        // If CONFIRMED and NOW > reservation_time + 15 minutes => CANCELED
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

        // Thread #2: Expire FINISHED reservations + free their tables:
        // If ARRIVED and NOW > reservation_time + 2 hours => EXPIRED
        // Then: set restaurant_table.status back to FREE for those EXPIRED reservations
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int expired = ReservationDAO.expireFinishedArrived2Hours();
                if (expired > 0) {
                    System.out.println("[JOB] EXPIRED finished ARRIVED (2 hours): " + expired);
                }

                // ✅ Option 1: free tables of expired reservations (prevents "no FREE tables forever")
                int freed = RestaurantTableDAO.freeTablesForExpiredReservations();
                if (freed > 0) {
                    System.out.println("[JOB] Freed tables for EXPIRED reservations: " + freed);
                }

            } catch (Exception e) {
                System.out.println("[JOB] expire/free error: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);

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
}



