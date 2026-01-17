package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;

import DataBase.dao.RestaurantTableDAO;
/**
 * Imports restaurant table data from a CSV (txt) file into the database.
 * <p>
 * Each record represents a physical table in the restaurant,
 * including table ID, number of seats, and current status.
 */
public class TxtRestaurantTableImporter {
	/**
	 * Reads restaurant table data from a text file and inserts it into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * <ul>
	 *   <li>Column 0 – table ID</li>
	 *   <li>Column 1 – number of seats</li>
	 *   <li>Column 2 – table status</li>
	 * </ul>
	 *
	 * @param filePath path to the restaurant table data file
	 * @throws Exception if file reading or database insertion fails
	 */
    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                String tableId = p[0];
                int seats = Integer.parseInt(p[1]);
                String status = p[2];

                RestaurantTableDAO.insertTable(
                        tableId,
                        seats,
                        status
                );
            }
        }
    }
}
