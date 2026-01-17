package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.UserActivityDAO;
/**
 * Imports user activity records from a CSV (txt) file into the user_activity table.
 * <p>
 * Supports both subscribers and guests, and handles optional reservation
 * and waiting list references.
 */
public class TxtUserActivityImporter {
	/**
	 * Reads user activity data from a text file and inserts it into the database.
	 * <p>
	 * Handles:
	 * <ul>
	 *   <li>Empty or whitespace lines</li>
	 *   <li>Optional reservation_id and waiting_id</li>
	 *   <li>UTF-8 BOM characters</li>
	 * </ul>
	 *
	 * Expected file format (comma-separated):
	 * subscriber_username, guest_phone, guest_email,
	 * reservation_id, waiting_id, activity_date
	 *
	 * @param filePath path to the user_activity data file
	 * @throws Exception if file reading, parsing, or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;
            int lineNumber = 0;

            // read header
            br.readLine();

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // ✅ FIX 1: skip empty or whitespace lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // ✅ FIX 2: remove BOM if exists
                line = line.replace("\uFEFF", "");

                String[] p = line.split(",", -1);

                // still invalid? skip with warning
                if (p.length < 7) {
                    System.out.println(
                        "⚠ Skipping invalid user_activity line " + (lineNumber + 1)
                        + " → " + line
                    );
                    continue;
                }

                Integer reservationId =
                        p[4].trim().isEmpty() ? null : Integer.parseInt(p[4].trim());

                Integer waitingId =
                        p[5].trim().isEmpty() ? null : Integer.parseInt(p[5].trim());

                UserActivityDAO.insertActivity(
                        p[1].trim().isEmpty() ? null : p[1].trim(),
                        p[2].trim().isEmpty() ? null : p[2].trim(),
                        p[3].trim().isEmpty() ? null : p[3].trim(),
                        reservationId,
                        waitingId,
                        Timestamp.valueOf(p[6].trim())
                );
            }
        }
    }
}
