package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.ReservationDAO;
/**
 * Imports reservation records from a CSV (txt) file into the database.
 * <p>
 * Each record represents a reservation request,
 * including reservation time, expiry time, status, and confirmation code.
 * <p>
 * The importer skips the header row and inserts all valid rows
 * into the reservation table.
 */
public class TxtReservationImporter {
	/**
	 * Reads reservation data from a text file and inserts it into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * <ul>
	 *   <li>Column 0 – ignored (ID or index)</li>
	 *   <li>Column 1 – number of customers</li>
	 *   <li>Column 2 – reservation time (yyyy-MM-dd HH:mm:ss)</li>
	 *   <li>Column 3 – expiry time (yyyy-MM-dd HH:mm:ss)</li>
	 *   <li>Column 4 – reservation status</li>
	 *   <li>Column 5 – confirmation code</li>
	 * </ul>
	 *
	 * @param filePath path to the reservation data file
	 * @throws Exception if file reading or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                int numOfCustomers = Integer.parseInt(p[1]);
                Timestamp reservationTime = Timestamp.valueOf(p[2]);
                Timestamp expiryTime = Timestamp.valueOf(p[3]);
                String status = p[4];
                String confirmationCode = p[5];

                ReservationDAO.insertReservation(
                        numOfCustomers,
                        reservationTime,
                        expiryTime,
                        status,
                        confirmationCode
                );
            }
        }
    }
}
