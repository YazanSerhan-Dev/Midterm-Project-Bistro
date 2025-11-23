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

        // DAO that talks with the Reservation table in the DB
        ReservationDAO reservationDAO = new ReservationDAO();

        try (ServerSocket serverSocket = new ServerSocket(5555)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // ----- show client details (required by assignment) -----
                InetAddress addr = clientSocket.getInetAddress();
                System.out.println("Client connected!");
                System.out.println("Client IP: " + addr.getHostAddress());
                System.out.println("Client host name: " + addr.getHostName());

                // Streams for talking with the client
                try (BufferedReader input = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter output = new PrintWriter(
                            clientSocket.getOutputStream(), true)) {

                    String message = input.readLine();
                    System.out.println("Received: " + message);

                    try {
                        // ---------- command: get all reservations ----------
                    	if ("GET_RESERVATIONS".equals(message)) {

                    	    List<Reservation> reservations = reservationDAO.getAllReservations();

                    	    if (reservations.isEmpty()) {
                    	        // tell client there is nothing
                    	        output.println("NO_RESERVATIONS");
                    	    } else {
                    	        // send one line per reservation
                    	        for (Reservation r : reservations) {
                    	            String line = r.getReservationNumber()
                    	                    + " | date=" + r.getReservationDate()
                    	                    + " | guests=" + r.getNumberOfGuests();
                    	            output.println(line);
                    	        }
                    	    }

                    	    // after this, while-loop on client will keep reading
                    	    // until socket closes and readLine() returns null
                    	}


                        // ---------- command: update reservation ----------
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

                        // ---------- unknown command ----------
                        else {
                            output.println("UNKNOWN_COMMAND");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        output.println("ERROR: " + ex.getMessage());
                    }
                }

                // close socket for this client (streams already closed)
                clientSocket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

