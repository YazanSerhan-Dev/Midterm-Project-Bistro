package DataBase.importer;

public class AllTablesImporter {

    // Folder that contains ALL txt files
    private static final String BASE_PATH =
            "C:/Users/yazan/git/Midterm-Project-Bistro/bistro_sample_data/";

    public static void main(String[] args) {

        try {
            System.out.println("=== START IMPORT ===");

            // ⚠️ ONLY FOR TESTING — remove in production
            DatabaseResetter.resetDatabase();

            // ===== 1) Base tables (no FK dependencies) =====
            TxtStaffImporter.importFromFile(BASE_PATH + "staff.txt");
            TxtSubscriberImporter.importFromFile(BASE_PATH + "subscribers.txt");
            TxtRestaurantTableImporter.importFromFile(BASE_PATH + "restaurant_table.txt");
            TxtOpeningHoursImporter.importFromFile(BASE_PATH + "opening_hours.txt");

            // ===== 2) Business flow =====
            TxtReservationImporter.importFromFile(BASE_PATH + "reservation.txt");
            TxtWaitingListImporter.importFromFile(BASE_PATH + "waiting_list.txt");

            // ===== 3) User activity mapping =====
            TxtUserActivityImporter.importFromFile(BASE_PATH + "user_activity.txt");

            // ===== 4) Visit =====
            TxtVisitImporter.importFromFile(BASE_PATH + "visit.txt");

            // ===== 5) Financial =====
            TxtBillImporter.importFromFile(BASE_PATH + "bill.txt");

            // ===== 6) Performance logs =====
            TxtPerformanceLogImporter.importFromFile(BASE_PATH + "performance_log.txt");

            System.out.println("=== ✅ ALL IMPORT DONE SUCCESSFULLY ===");

        } catch (Exception e) {
            System.out.println("=== ❌ IMPORT FAILED ===");
            e.printStackTrace();
        }
    }
}
