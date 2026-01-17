package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import DataBase.dao.StaffDAO;
/**
 * Imports staff users from a CSV (txt) file into the staff table.
 * <p>
 * Each row represents one staff member with login and role details.
 */
public class TxtStaffImporter {
	/**
	 * Reads staff data from a text file and inserts it into the database.
	 * <p>
	 * Expected file format (comma-separated):
	 * <ul>
	 *   <li>Column 1 – username</li>
	 *   <li>Column 2 – password</li>
	 *   <li>Column 3 – role</li>
	 *   <li>Column 4 – full name</li>
	 * </ul>
	 *
	 * @param path path to the staff data file
	 * @throws Exception if file reading or database insertion fails
	 */
    public static void importFromFile(String path) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // header
            String line;

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                StaffDAO.insertStaff(p[1], p[2], p[3], p[4]);
            }
        }
    }
}
