package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.ReservationDAO;

public class TxtReservationImporter {

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
