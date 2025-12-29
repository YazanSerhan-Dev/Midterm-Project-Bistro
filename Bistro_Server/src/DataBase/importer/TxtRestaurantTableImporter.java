package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;

import DataBase.dao.RestaurantTableDAO;

public class TxtRestaurantTableImporter {

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
