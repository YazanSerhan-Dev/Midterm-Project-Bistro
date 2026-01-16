package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.WaitingListDAO;

public class TxtWaitingListImporter {

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
