package Server;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class ServerController {

    @FXML private TextArea logArea;
    @FXML private Button startButton;
    @FXML private Button exitButton;   // כפתור עצירה

    private BistroServer server;

    @FXML
    private void initialize() {
        appendLogFromServer("Server ready. Click 'Start Server'.\n");

        // בתחילת הדרך – אפשר להתחיל שרת, אי אפשר לעצור
        if (startButton != null) {
            startButton.setDisable(false);
        }
        if (exitButton != null) {
            exitButton.setDisable(true);
        }
    }

    @FXML
    private void onStartServer() {
        if (server != null && server.isListening()) {
            appendLogFromServer("Server is already running.\n");
            return;
        }

        try {
            int port = 5555;  // הפורט של התרגיל
            server = new BistroServer(port, this);
            server.listen();            // OCSF start
            appendLogFromServer("Server starting on port " + port + "...\n");

            // אחרי שהשרת התחיל – אי אפשר ללחוץ שוב על Start
            if (startButton != null) {
                startButton.setDisable(true);
            }
            if (exitButton != null) {
                exitButton.setDisable(false);
            }
        } catch (Exception e) {
            appendLogFromServer("Error starting server: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    @FXML
    private void onExitServer() {
        try {
            if (server != null) {
                if (server.isListening()) {
                    server.stopListening();
                }
                server.close();  // סוגר את כל החיבורים והשרת
                appendLogFromServer("Server stopped.\n");
            } else {
                appendLogFromServer("Server is not running.\n");
            }
        } catch (IOException e) {
            appendLogFromServer("Error stopping server: " + e.getMessage() + "\n");
            e.printStackTrace();
        } finally {
            // אחרי עצירה – אפשר שוב Start, אי אפשר Stop
            if (startButton != null) {
                startButton.setDisable(false);
            }
            if (exitButton != null) {
                exitButton.setDisable(true);
            }
        }
    }

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




