package Server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class ServerController {

    @FXML
    private TextArea logArea;

    @FXML
    private Button startButton;

    private final int PORT = 5555;
    private volatile boolean running = false;


    @FXML
    private void initialize() {
        if (logArea != null) {
            logArea.appendText("Server ready. Click 'Start Server'.\n");
        }
    }

    @FXML
    private void onStartServer(ActionEvent event) {
        if (running) {
            appendLog("Server already running.\n");
            return;
        }

        running = true;
        startButton.setDisable(true);
        appendLog("Server starting on port " + PORT + "...\n");

        Thread t = new Thread(this::serverLoop);
        t.setDaemon(true);
        t.start();
    }

    private void serverLoop() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            appendLog("Server listening on port " + PORT + "\n");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        } catch (Exception e) {
            appendLog("Server error: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // somewhere in your ServerFX / ServerController class:
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private void handleClient(Socket clientSocket) {
        new Thread(() -> {
            try (Socket socket = clientSocket;
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                appendLog("Client connected - IP: " + socket.getInetAddress() + "\n");

                String line = in.readLine();
                appendLog("Received: " + line + "\n");

                if (line == null) {
                    appendLog("Empty command from client\n");
                    return;
                }

                // -----------------------------------------
                // 1) GET_RESERVATIONS  -> send list from DB
                // -----------------------------------------
                if ("GET_RESERVATIONS".equals(line)) {

                    // get all reservations from DB  (change method name if needed)
                    List<Reservation> reservations = reservationDAO.getAllReservations();

                    StringBuilder sb = new StringBuilder();
                    for (Reservation r : reservations) {
                        sb.append(r.getReservationNumber())
                          .append(" | date=")
                          .append(r.getReservationDate())
                          .append(" | guests=")
                          .append(r.getNumberOfGuests())
                          .append("\n");
                    }

                    String reply = (sb.length() == 0)
                            ? "NO_RESERVATIONS"
                            : sb.toString().trim();

                    out.println(reply);
                    appendLog("Sent " + reservations.size() + " reservations\n");
                }

                // ---------------------------------------------------------
                // 2) UPDATE_RESERVATION:<num>:<yyyy-MM-dd>:<guests>
                // ---------------------------------------------------------
                else if (line.startsWith("UPDATE_RESERVATION:")) {

                    String[] parts = line.split(":");
                    if (parts.length == 4) {

                        int orderNumber = Integer.parseInt(parts[1]);
                        String newDate = parts[2];
                        int guests = Integer.parseInt(parts[3]);

                        try {
                            // update DB
                            reservationDAO.updateReservation(orderNumber, java.sql.Date.valueOf(newDate), guests);

                            out.println("UPDATE_OK");
                            appendLog("Updated reservation " + orderNumber);

                        } catch (SQLException e) {
                            out.println("UPDATE_ERROR");
                            appendLog("Update failed: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                // -----------------------------
                // 3) Unknown command
                // -----------------------------
                else {
                    out.println("ERROR_UNKNOWN_COMMAND");
                    appendLog("Unknown command: " + line + "\n");
                }

            } catch (Exception e) {
                appendLog("Client error: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }).start();
    }


    private void appendLog(String text) {
        Platform.runLater(() -> logArea.appendText(text));
    }
}


