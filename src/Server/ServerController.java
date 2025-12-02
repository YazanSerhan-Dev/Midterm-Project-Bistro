package Server;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class ServerController {

    @FXML private TextArea logArea;
    @FXML private Button startButton;
    @FXML private Button exitButton;   // new exit/stop button

    private BistroServer server;

    @FXML
    private void initialize() {
        // Initial UI state when the window is opened
        appendLogFromServer("Server ready. Click 'Start Server'.");
        if (startButton != null) {
            startButton.setDisable(false);  // can start
        }
        if (exitButton != null) {
            exitButton.setDisable(true);    // cannot stop until started
        }
    }

    @FXML
    private void onStartServer() {
        // If server is already running, do nothing
        if (server != null && server.isListening()) {
            appendLogFromServer("Server is already running.");
            return;
        }

        try {
            int port = 5555; // fixed port for the assignment
            // Create server and start listening (OCSF)
            server = new BistroServer(port, this);
            server.listen();

            appendLogFromServer("Server started on port " + port + ".");

            // Disable start button so it cannot be pressed again
            if (startButton != null) {
                startButton.setDisable(true);
            }
            // Enable exit/stop button
            if (exitButton != null) {
                exitButton.setDisable(false);
            }
        } catch (IOException e) {
            appendLogFromServer("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onExitServer() {
        try {
            if (server != null && server.isListening()) {
                // Stop accepting new clients and close connections
                server.stopListening();
                server.close();
                appendLogFromServer("Server stopped.");
            } else {
                appendLogFromServer("Server is not running.");
            }
        } catch (IOException e) {
            appendLogFromServer("Error stopping server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Allow starting the server again
            if (startButton != null) {
                startButton.setDisable(false);
            }
            // Disable exit button when server is not running
            if (exitButton != null) {
                exitButton.setDisable(true);
            }
        }
    }

    /**
     * Called from BistroServer to append messages to the log area.
     */
    public void appendLogFromServer(String msg) {
        javafx.application.Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(msg + "\n");
            } else {
                System.out.println(msg);
            }
        });
    }
}


