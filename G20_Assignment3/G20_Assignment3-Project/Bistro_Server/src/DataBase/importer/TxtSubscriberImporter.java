package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Date;

import DataBase.dao.SubscriberDAO;
/**
 * Imports subscribers data from a CSV (txt) file into the subscribers table.
 * <p>
 * Validates birth date values before inserting into the database.
 */
public class TxtSubscriberImporter {
	/**
	 * Reads subscriber records from a text file and inserts them into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * <ul>
	 *   <li>username</li>
	 *   <li>password</li>
	 *   <li>name</li>
	 *   <li>phone</li>
	 *   <li>email</li>
	 *   <li>...</li>
	 *   <li>birth_date (YYYY-MM-DD)</li>
	 * </ul>
	 *
	 * @param filePath path to the subscribers data file
	 * @throws Exception if file reading, validation, or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;
            int lineNumber = 1;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                String[] p = line.split(",", -1);

                String birthDateStr = p[7].trim();

                if (birthDateStr.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Invalid birth_date at line " + lineNumber +
                        " (empty value)"
                    );
                }

                Date birthDate;
                try {
                    birthDate = Date.valueOf(birthDateStr);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        "Invalid birth_date format at line " + lineNumber +
                        ": " + birthDateStr +
                        " (expected YYYY-MM-DD)"
                    );
                }

                SubscriberDAO.insertSubscriber(
                        p[0].trim(),
                        p[1].trim(),
                        p[2].trim(),
                        p[3].trim(),
                        p[4].trim(),
                        birthDate
                );
            }
        }
    }
}
