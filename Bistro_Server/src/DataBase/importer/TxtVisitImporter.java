package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.VisitDAO;
/**
 * Imports visit records from a CSV (txt) file into the visit table.
 * <p>
 * Each record represents a seated visit linked to a user activity
 * and includes table assignment and start/end times.
 */
public class TxtVisitImporter {
	/**
	 * Reads visit data from a text file and inserts it into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * activity_id, table_id, actual_start_time, actual_end_time
	 *
	 * @param filePath path to the visit data file
	 * @throws Exception if file reading, parsing, or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                int activityId = Integer.parseInt(p[1]);
                String tableId = p[2];

                Timestamp startTime = Timestamp.valueOf(p[3]);
                Timestamp endTime   = Timestamp.valueOf(p[4]);

                VisitDAO.insertVisit(
                        activityId,
                        tableId,
                        startTime,
                        endTime
                );
            }
        }
    }
}
