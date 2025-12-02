package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ClientController {

    // ===== OCSF client =====
    private BistroClient client;

    // kept for clarity – actual port is taken from ClientFX.getPort()
    private static final int SERVER_PORT = 5555;

    // current host we are using
    private String SERVER_HOST = "127.0.0.1";

    // ===== FXML fields =====
    @FXML private TextArea reservationsArea;
    @FXML private TextField reservationNumberField;
    @FXML private TextField dateField;
    @FXML private TextField guestsField;
    @FXML private TextField ipField;          // new: lets user change IP from the UI
    @FXML private Label statusLabel;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;

    // ----- connection management -----
    public void connectToServer() {
        try {
            // If we are already connected – do nothing
            if (client != null && client.isConnected()) {
                statusLabel.setText("Already connected to " + SERVER_HOST + ":" + ClientFX.getPort());
                return;
            }

            // Decide which host to use: value from the text field, or the one from ClientFX
            String hostFromArgs = ClientFX.getHost();
            String hostFromUI = (ipField != null && ipField.getText() != null)
                    ? ipField.getText().trim()
                    : "";

            if (!hostFromUI.isEmpty()) {
                SERVER_HOST = hostFromUI;
            } else {
                SERVER_HOST = hostFromArgs;
            }

            client = new BistroClient(SERVER_HOST, ClientFX.getPort(), this);
            statusLabel.setText("Connecting to " + SERVER_HOST + ":" + ClientFX.getPort() + "...");

            client.openConnection();

        } catch (Exception e) {
            statusLabel.setText("Connection failed: " + e.getMessage());
            e.printStackTrace();
            client = null;
        }
    }

    public void disconnectFromServer() {
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();   // triggers connectionClosed() in BistroClient
                statusLabel.setText("Disconnected from server");
            } else {
                statusLabel.setText("Not connected");
            }
        } catch (Exception e) {
            statusLabel.setText("Error while disconnecting: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onConnected() {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText("Connected to " + SERVER_HOST + " via port " + ClientFX.getPort());
            if (connectButton != null) {
                connectButton.setDisable(true);
            }
            if (disconnectButton != null) {
                disconnectButton.setDisable(false);
            }
        });
    }

    public void onConnectionError(Exception e) {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText("Connection error: " + e.getMessage());
            if (disconnectButton != null) {
                disconnectButton.setDisable(true);
            }
            if (connectButton != null) {
                connectButton.setDisable(false);
            }
            client = null;
        });
    }

    public void onDisconnected() {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText("Disconnected from server");
            if (disconnectButton != null) {
                disconnectButton.setDisable(true);
            }
            if (connectButton != null) {
                connectButton.setDisable(false);
            }
            client = null;
        });
    }

    // called from BistroClient when a message arrives
    public void handleServerMessage(Object msg) {
        String response = String.valueOf(msg);

        javafx.application.Platform.runLater(() -> {
            reservationsArea.setText(response);
            statusLabel.setText("Message from server");
        });
    }

    // called automatically when FXML is loaded
    @FXML
    private void initialize() {
        // initialize IP field with whatever ClientFX decided (either default or from args)
        if (ipField != null) {
            ipField.setText("127.0.0.1");
        }
        if (statusLabel != null) {
            statusLabel.setText("Not connected");
        }
        if (disconnectButton != null) {
            disconnectButton.setDisable(true);
        }
    }

    // ===== Button handlers =====

    @FXML
    private void onConnect(ActionEvent event) {
        connectToServer();
    }

    @FXML
    private void onDisconnect(ActionEvent event) {
        disconnectFromServer();
    }

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




