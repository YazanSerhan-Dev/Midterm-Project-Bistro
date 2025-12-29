package DataBase.importer;

import java.io.BufferedReader;
import java.io.FileReader;

import DataBase.dao.BillDAO;

public class TxtBillImporter {

    public static void importFromFile(String filePath) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            br.readLine(); // skip header
            String line;

            while ((line = br.readLine()) != null) {

                String[] p = line.split(",", -1);

                int visitId = Integer.parseInt(p[1]);
                double totalAmount = Double.parseDouble(p[2]);
                String isSubscriberDiscount = p[3];
                String isPaid = p[4];

                BillDAO.insertBill(
                        visitId,
                        totalAmount,
                        isSubscriberDiscount,
                        isPaid
                );
            }
        }
    }
}
