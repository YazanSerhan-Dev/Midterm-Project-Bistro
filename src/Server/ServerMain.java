package Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.util.List;

public class ServerMain {

    public static void main(String[] args) {
        System.out.println("Server started...");

        // DAO that talks with the DB table `order`
        ReservationDAO reservationDAO = new ReservationDAO();

        try (ServerSocket serverSocket = new ServerSocket(5555)) {
            System.out.println("Waiting for client on port 5555...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // ---- show client details (assignment requirement) ----
                InetAddress addr = clientSocket.getInetAddress();
                System.out.println("Client connected!");
                System.out.println("Client IP: " + addr.getHostAddress());
                System.out.println("Client host name: " + addr.getHostName());

                try (BufferedReader input = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter output = new PrintWriter(
                            clientSocket.getOutputStream(), true)) {

                    String message = input.readLine();
                    System.out.println("Received: " + message);

                    try {
                        // ================= GET_RESERVATIONS =================
                        if ("GET_RESERVATIONS".equals(message)) {

                            List<Reservation> reservations =
                                    reservationDAO.getAllReservations();

                            if (reservations.isEmpty()) {
                                output.println("NO_RESERVATIONS");
                            } else {
                                for (Reservation r : reservations) {
                                    String line = r.getReservationNumber()
                                            + " | date=" + r.getReservationDate()
                                            + " | guests=" + r.getNumberOfGuests();
                                    output.println(line);
                                }
                            }
                        }

                        // ============== UPDATE_RESERVATION ==================
                        // format: UPDATE_RESERVATION:number:yyyy-mm-dd:guests
                        else if (message != null &&
                                 message.startsWith("UPDATE_RESERVATION:")) {

                            String[] parts = message.split(":");
                            if (parts.length == 4) {
                                int reservationNumber = Integer.parseInt(parts[1]);
                                Date newDate = Date.valueOf(parts[2]); // yyyy-mm-dd
                                int guests = Integer.parseInt(parts[3]);

                                reservationDAO.updateReservation(
                                        reservationNumber, newDate, guests);

                                output.println("UPDATE_OK");
                            } else {
                                output.println("ERROR: bad UPDATE_RESERVATION format");
                            }
                        }

                        // ================= UNKNOWN COMMAND ==================
                        else {
                            output.println("UNKNOWN_COMMAND");
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        output.println("ERROR: " + ex.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception ignore) {}
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


