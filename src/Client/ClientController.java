package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ClientController {

    // ===== OCSF client =====
    private BistroClient client;

    private static final int SERVER_PORT = 5555;
    // change this IP when running on 2 different PCs
    private String SERVER_HOST = "127.0.0.1";

    // optional helper if you want to set IP from outside
    public void setServerHost(String host) {
        this.SERVER_HOST = host;
    }

    // ----- connection management -----
    public void connectToServer() {
        try {
            String ip = (serverIpField != null) ? serverIpField.getText() : null;

            if (ip == null || ip.isBlank()) {
                ip = "127.0.0.1"; // fallback default
            }

            SERVER_HOST = ip.trim();

            client = new BistroClient(SERVER_HOST, SERVER_PORT, this);
            client.openConnection();

            statusLabel.setText("Connecting to " + SERVER_HOST + "...");
        } catch (Exception e) {
            statusLabel.setText("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void onConnected() {
        javafx.application.Platform.runLater(
                () -> statusLabel.setText("Connected to server")
        );
    }

    public void onConnectionError(Exception e) {
        javafx.application.Platform.runLater(
                () -> statusLabel.setText("Connection error: " + e.getMessage())
        );
    }

    public void onDisconnected() {
        javafx.application.Platform.runLater(
                () -> statusLabel.setText("Disconnected from server")
        );
    }

    // called from BistroClient when a message arrives
    public void handleServerMessage(Object msg) {
        String response = String.valueOf(msg);

        javafx.application.Platform.runLater(() -> {
            reservationsArea.setText(response);
            statusLabel.setText("Message from server");
        });
    }

    // ===== FXML fields =====
    @FXML private TextArea reservationsArea;
    @FXML private TextField reservationNumberField;
    @FXML private TextField dateField;
    @FXML private TextField guestsField;
    @FXML private Label statusLabel;
    @FXML private TextField serverIpField;

    // called automatically when FXML is loaded
    @FXML
    private void initialize() {
        if (serverIpField != null) {
            serverIpField.setText("127.0.0.1"); // default when testing on same PC
        }
        statusLabel.setText("Not connected");
    }


    @FXML
    private void onConnect(ActionEvent event) {
        connectToServer();
    }

    // ===== Button handlers =====
    @FXML
    private void onGetReservations(ActionEvent event) {
        try {
            if (client == null || !client.isConnected()) {
                statusLabel.setText("Not connected to server");
                return;
            }

            client.sendToServer("GET_RESERVATIONS");

        } catch (Exception e) {
            statusLabel.setText("Error sending GET: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onUpdateReservation(ActionEvent event) {
        try {
            if (client == null || !client.isConnected()) {
                statusLabel.setText("Not connected to server");
                return;
            }

            int num    = Integer.parseInt(reservationNumberField.getText());
            String date = dateField.getText();
            int guests = Integer.parseInt(guestsField.getText());

            String cmd = "UPDATE_RESERVATION:" + num + ":" + date + ":" + guests;
            client.sendToServer(cmd);

        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid number / guests");
        } catch (Exception e) {
            statusLabel.setText("Error sending update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


