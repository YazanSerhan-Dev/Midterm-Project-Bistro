package Server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.util.List;

public class ServerFX extends Application {

    private TextArea logArea;
    private Button startButton;

    private final int PORT = 5555;
    private volatile boolean running = false;

    private ReservationDAO reservationDAO = new ReservationDAO();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Bistro Server");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(15);

        startButton = new Button("Start Server");
        startButton.setOnAction(e -> startServerThread());

        VBox root = new VBox(10,
                new Label("Server log:"),
                logArea,
                startButton
        );
        root.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(root, 700, 400));
        primaryStage.show();
    }

    private void startServerThread() {
        if (running) return;

        running = true;
        startButton.setDisable(true);
        appendLog("Server starting on port " + PORT + "...");

        new Thread(this::runServerLoop).start();
    }

    private void runServerLoop() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            appendLog("Server listening on port " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();

                InetAddress addr = clientSocket.getInetAddress();
                appendLog("Client connected - IP: " + addr.getHostAddress()
                        + ", host: " + addr.getHostName());

                // Handle each client in its own thread
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (Exception e) {
            appendLog("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader input = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(
                    clientSocket.getOutputStream(), true)) {

            String message = input.readLine();
            appendLog("Received: " + message);

            try {
                if ("GET_RESERVATIONS".equals(message)) {
                    List<Reservation> reservations =
                            reservationDAO.getAllReservations();

                    if (reservations.isEmpty()) {
                        output.println("NO_RESERVATIONS");
                        appendLog("Sent: NO_RESERVATIONS");
                    } else {
                        for (Reservation r : reservations) {
                            String line = r.getReservationNumber()
                                    + " | date=" + r.getReservationDate()
                                    + " | guests=" + r.getNumberOfGuests();
                            output.println(line);
                        }
                        appendLog("Sent " + reservations.size() + " reservations");
                    }
                }
                else if (message != null &&
                         message.startsWith("UPDATE_RESERVATION:")) {

                    String[] parts = message.split(":");
                    if (parts.length == 4) {
                        int reservationNumber = Integer.parseInt(parts[1]);
                        Date newDate = Date.valueOf(parts[2]);
                        int guests = Integer.parseInt(parts[3]);

                        reservationDAO.updateReservation(
                                reservationNumber, newDate, guests);

                        output.println("UPDATE_OK");
                        appendLog("Updated reservation #" + reservationNumber +
                                " to date=" + newDate + ", guests=" + guests);
                    } else {
                        output.println("ERROR: bad UPDATE_RESERVATION format");
                        appendLog("Bad UPDATE_RESERVATION format from client");
                    }
                }
                else {
                    output.println("UNKNOWN_COMMAND");
                    appendLog("Unknown command from client");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                output.println("ERROR: " + ex.getMessage());
                appendLog("Error while handling client: " + ex.getMessage());
            }

        } catch (Exception e) {
            appendLog("Client connection error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception ignore) {}
        }
    }

    private void appendLog(String text) {
        Platform.runLater(() -> {
            logArea.appendText(text + "\n");
        });
    }
}

