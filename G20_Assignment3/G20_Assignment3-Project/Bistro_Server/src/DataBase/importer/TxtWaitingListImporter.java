package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.WaitingListDAO;
/**
 * Imports waiting list records from a CSV (txt) file into the waiting_list table.
 * <p>
 * Each record represents a waiting list request with number of customers,
 * request time, status, and confirmation code.
 */
public class TxtWaitingListImporter {
	/**
	 * Reads waiting list data from a text file and inserts it into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * num_of_customers, request_time, status, confirmation_code
	 *
	 * @param filePath path to the waiting list data file
	 * @throws Exception if file reading, parsing, or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                int numOfCustomers = Integer.parseInt(p[1]);
                Timestamp requestTime = Timestamp.valueOf(p[2]);
                String status = p[3];
                String confirmationCode = p[4];

                WaitingListDAO.insertWaiting(
                        numOfCustomers,
                        requestTime,
                        status,
                        confirmationCode
                );
            }
        }
    }
}
