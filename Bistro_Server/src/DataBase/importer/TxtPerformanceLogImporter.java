package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Date;

import DataBase.dao.PerformanceLogDAO;

public class TxtPerformanceLogImporter {

    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                int visitId = Integer.parseInt(p[1]);
                int lateMinutes = Integer.parseInt(p[2]);
                int overstayMinutes = Integer.parseInt(p[3]);
                Date reportDate = Date.valueOf(p[4]);

                PerformanceLogDAO.insertLog(
                        visitId,
                        lateMinutes,
                        overstayMinutes,
                        reportDate
                );
            }
        }
    }
}
