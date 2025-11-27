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

    private void handleClient(Socket clientSocket) {
        new Thread(() -> {
            try (Socket socket = clientSocket;
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                appendLog("Client connected - IP: " + socket.getInetAddress() + "\n");

                String line = in.readLine();
                appendLog("Received: " + line + "\n");

                // TODO: here paste your existing logic:
                // - if line.equals("GET_RESERVATIONS") -> use ReservationDAO to read DB,
                //   build a string, and send it with out.println(...)
                // - else if it startsWith("UPDATE_RESERVATION:") -> parse, update DB, out.println("UPDATE_OK")

                out.println("OK");
                appendLog("Sent reply: OK\n");

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


