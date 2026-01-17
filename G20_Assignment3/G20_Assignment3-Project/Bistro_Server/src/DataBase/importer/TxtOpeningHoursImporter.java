package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Time;
import java.sql.Date;

import DataBase.dao.OpeningHoursDAO;
/**
 * Imports opening hours data from a CSV (txt) file into the database.
 * <p>
 * Each line represents opening hours for a specific day of the week,
 * including optional special dates (holidays / exceptions).
 * <p>
 * The importer skips the header row and processes all remaining entries.
 */
public class TxtOpeningHoursImporter {
	/**
	 * Reads opening hours records from a text file and inserts them into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * <ul>
	 *   <li>Column 0 – ignored (ID or index)</li>
	 *   <li>Column 1 – day of week</li>
	 *   <li>Column 2 – opening time (HH:mm:ss)</li>
	 *   <li>Column 3 – closing time (HH:mm:ss)</li>
	 *   <li>Column 4 – special flag</li>
	 *   <li>Column 5 – special date (optional, yyyy-MM-dd)</li>
	 * </ul>
	 *
	 * @param filePath path to the opening hours data file
	 * @throws Exception if file reading or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                String dayOfWeek = p[1];
                Time openTime = Time.valueOf(p[2]);
                Time closeTime = Time.valueOf(p[3]);
                String isSpecial = p[4];
             // Optional special date (can be empty)
                Date specialDate =
                        p[5].isEmpty() ? null : Date.valueOf(p[5]);

                OpeningHoursDAO.insertOpeningHours(
                        dayOfWeek,
                        openTime,
                        closeTime,
                        isSpecial,
                        specialDate
                );
            }
        }
    }
}
