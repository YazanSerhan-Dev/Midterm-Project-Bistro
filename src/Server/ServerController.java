package Server;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ServerController {

    @FXML private TextArea logArea;
    @FXML private Button startButton;

    private BistroServer server;

    @FXML
    private void initialize() {
        appendLogFromServer("Server ready. Click 'Start Server'.\n");
    }

    @FXML
    private void onStartServer() {
        if (server != null && server.isListening()) {
            appendLogFromServer("Server is already running.\n");
            return;
        }

        try {
            int port = 5555;
            server = new BistroServer(port, this);
            server.listen();  // OCSF start
            appendLogFromServer("Server starting on port " + port + "...\n");
        } catch (Exception e) {
            appendLogFromServer("Error starting server: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    public void appendLogFromServer(String msg) {
        if (logArea != null) {
            logArea.appendText(msg);
        } else {
            System.out.print(msg);
        }
    }
}


