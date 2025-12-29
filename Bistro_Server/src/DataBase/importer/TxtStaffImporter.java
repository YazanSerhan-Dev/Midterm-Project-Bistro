package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import DataBase.dao.StaffDAO;

public class TxtStaffImporter {

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
