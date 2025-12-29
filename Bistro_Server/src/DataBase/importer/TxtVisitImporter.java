package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.VisitDAO;

public class TxtVisitImporter {

    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                int activityId = Integer.parseInt(p[1]);
                String tableId = p[2];

                Timestamp startTime = Timestamp.valueOf(p[3]);
                Timestamp endTime   = Timestamp.valueOf(p[4]);

                VisitDAO.insertVisit(
                        activityId,
                        tableId,
                        startTime,
                        endTime
                );
            }
        }
    }
}
