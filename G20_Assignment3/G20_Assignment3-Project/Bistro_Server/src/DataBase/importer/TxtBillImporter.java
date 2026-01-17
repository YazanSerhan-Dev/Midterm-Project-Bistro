package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;

import DataBase.dao.BillDAO;
/**
 * Imports bill records from a CSV (txt) file into the database.
 * <p>
 * Each line in the file represents a single bill entry
 * and is mapped directly to the {@code bill} table.
 * <p>
 * The importer skips the header line and processes all remaining rows.
 */
public class TxtBillImporter {
	/**
	 * Reads bill data from a text file and inserts it into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * <ul>
	 *   <li>Column 0 – ignored (ID or index)</li>
	 *   <li>Column 1 – visit ID</li>
	 *   <li>Column 2 – total amount</li>
	 *   <li>Column 3 – subscriber discount flag</li>
	 *   <li>Column 4 – payment status</li>
	 * </ul>
	 *
	 * @param filePath path to the bill data file
	 * @throws Exception if file reading or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {
            	// Split line including empty values
                String[] p = line.split(",", -1);

                int visitId = Integer.parseInt(p[1]);
                double totalAmount = Double.parseDouble(p[2]);
                String isSubscriberDiscount = p[3];
                String isPaid = p[4];

                BillDAO.insertBill(
                        visitId,
                        totalAmount,
                        isSubscriberDiscount,
                        isPaid
                );
            }
        }
    }
}
