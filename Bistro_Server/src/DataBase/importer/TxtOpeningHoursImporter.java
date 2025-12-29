package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Time;
import java.sql.Date;

import DataBase.dao.OpeningHoursDAO;

public class TxtOpeningHoursImporter {

    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                String dayOfWeek = p[1];
                Time openTime = Time.valueOf(p[2]);
                Time closeTime = Time.valueOf(p[3]);
                String isSpecial = p[4];

                Date specialDate =
                        p[5].isEmpty() ? null : Date.valueOf(p[5]);

                OpeningHoursDAO.insertOpeningHours(
                        dayOfWeek,
                        openTime,
                        closeTime,
                        isSpecial,
                        specialDate
                );
            }
        }
    }
}
