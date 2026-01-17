package DataBase.importer;
/**
 * Central importer runner for loading all system tables from TXT files.
 * <p>
 * Executes all importers in the correct order according to
 * database foreign-key dependencies.
 * Intended for initial data seeding and testing environments.
 */
public class AllTablesImporter {

    // Folder that contains ALL txt files
	/**
	 * Base folder containing all TXT data files.
	 * <p>
	 * Each importer receives a full file path built from this base.
	 */
    private static final String BASE_PATH =
    		"C:/Users/yazan/git/Midterm-Project-Bistro/bistro_sample_data/";
    /**
     * Runs the full database import process.
     * <p>
     * Flow:
     * <ul>
     *   <li>Optionally resets the database (testing only)</li>
     *   <li>Imports base tables without dependencies</li>
     *   <li>Imports business entities (reservations, waiting list)</li>
     *   <li>Imports user activity and visit mapping</li>
     *   <li>Imports billing and performance logs</li>
     * </ul>
     *
     * @param args not used
     */
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
