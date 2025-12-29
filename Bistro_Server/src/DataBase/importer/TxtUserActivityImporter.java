package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;

import DataBase.dao.UserActivityDAO;

public class TxtUserActivityImporter {

    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;
            int lineNumber = 0;

            // read header
            br.readLine();

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // ✅ FIX 1: skip empty or whitespace lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // ✅ FIX 2: remove BOM if exists
                line = line.replace("\uFEFF", "");

                String[] p = line.split(",", -1);

                // still invalid? skip with warning
                if (p.length < 7) {
                    System.out.println(
                        "⚠ Skipping invalid user_activity line " + (lineNumber + 1)
                        + " → " + line
                    );
                    continue;
                }

                Integer reservationId =
                        p[4].trim().isEmpty() ? null : Integer.parseInt(p[4].trim());

                Integer waitingId =
                        p[5].trim().isEmpty() ? null : Integer.parseInt(p[5].trim());

                UserActivityDAO.insertActivity(
                        p[1].trim().isEmpty() ? null : p[1].trim(),
                        p[2].trim().isEmpty() ? null : p[2].trim(),
                        p[3].trim().isEmpty() ? null : p[3].trim(),
                        reservationId,
                        waitingId,
                        Timestamp.valueOf(p[6].trim())
                );
            }
        }
    }
}
