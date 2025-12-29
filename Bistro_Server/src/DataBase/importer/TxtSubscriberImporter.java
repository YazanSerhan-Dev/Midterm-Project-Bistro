package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Date;

import DataBase.dao.SubscriberDAO;

public class TxtSubscriberImporter {

    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;
            int lineNumber = 1;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                String[] p = line.split(",", -1);

                String birthDateStr = p[7].trim();

                if (birthDateStr.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Invalid birth_date at line " + lineNumber +
                        " (empty value)"
                    );
                }

                Date birthDate;
                try {
                    birthDate = Date.valueOf(birthDateStr);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        "Invalid birth_date format at line " + lineNumber +
                        ": " + birthDateStr +
                        " (expected YYYY-MM-DD)"
                    );
                }

                SubscriberDAO.insertSubscriber(
                        p[0].trim(),
                        p[1].trim(),
                        p[2].trim(),
                        p[3].trim(),
                        p[4].trim(),
                        p[5].trim(),
                        p[6].trim(),
                        birthDate
                );
            }
        }
    }
}
