package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the Bistro client window.
 * - Manages BistroClient (OCSF)
 * - Handles all GUI actions (connect, disconnect, get, update)
 * - Updates the UI on connection / disconnection / server messages
 */
public class ClientController {

    // ===== OCSF client instance =====
    private BistroClient client;

    // current host we are using (default; may be overridden by args or UI)
    private String serverHost = "127.0.0.1";

    // ===== FXML fields =====
    @FXML private TextArea reservationsArea;
    @FXML private TextField reservationNumberField;
    @FXML private TextField dateField;
    @FXML private TextField guestsField;
    @FXML private TextField ipField;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;

    // ---------------------------------------------------------------------
    //  Connection management (non-FXML public methods, used by buttons & FX)
    // ---------------------------------------------------------------------

    /** Try to connect to the server, using IP from UI or from program args. */
    public void connectToServer() {
        try {
            // Already connected?
            if (client != null && client.isConnected()) {
                statusLabel.setText("Already connected to "
                        + serverHost + ":" + ClientFX.getPort());
                return;
            }

            // Decide which host to use: UI text field or value from ClientFX
            String hostFromArgs = ClientFX.getHost();
            String hostFromUI = (ipField != null && ipField.getText() != null)
                    ? ipField.getText().trim()
                    : "";

            if (!hostFromUI.isEmpty()) {
                serverHost = hostFromUI;
            } else {
                serverHost = hostFromArgs;
            }

            client = new BistroClient(serverHost, ClientFX.getPort(), this);

            statusLabel.setText(
                    "Connecting to " + serverHost + ":" + ClientFX.getPort() + "...");

            client.openConnection();  // async connect – callbacks will fire

        } catch (Exception e) {
            statusLabel.setText("Connection failed: " + e.getMessage());
            e.printStackTrace();
            client = null;
        }
    }

    /** Disconnect from server if connected. Called from button & when window closes. */
    public void disconnectFromServer() {
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();   // triggers connectionClosed()
                statusLabel.setText("Disconnected from server");
            } else {
                statusLabel.setText("Not connected");
            }
        } catch (Exception e) {
            statusLabel.setText("Error disconnecting: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------
    //  Callbacks from BistroClient (OCSF events)
    // ---------------------------------------------------------------------

    /** Called when connectionEstablished() fires in BistroClient. */
    public void onConnected() {
        javafx.application.Platform.runLater(() -> {
            statusLabel.setText("Connected to " + serverHost
                                + ":" + ClientFX.getPort());
            if (connectButton != null) {
                connectButton.setDisable(true);
            }
            if (disconnectButton != null) {
                disconnectButton.setDisable(false);
            }
        });
    }

    /** Called when connectionException() fires in BistroClient. */
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

    /** Called when connectionClosed() fires in BistroClient. */
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

    /** Called from BistroClient whenever a message arrives from the server. */
    public void handleServerMessage(Object msg) {
        String response = String.valueOf(msg);

        javafx.application.Platform.runLater(() -> {
            reservationsArea.setText(response);
            statusLabel.setText("Message from server");
        });
    }

    // ---------------------------------------------------------------------
    //  FXML lifecycle
    // ---------------------------------------------------------------------

    /** Called automatically when FXML is loaded. */
    @FXML
    private void initialize() {
        // Initialize IP field with default or later the user can change it
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

    // ---------------------------------------------------------------------
    //  Button handlers (wired in ClientView.fxml)
    // ---------------------------------------------------------------------

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

            // Validate basic fields
            if (reservationNumberField.getText().isEmpty()
                    || dateField.getText().isEmpty()
                    || guestsField.getText().isEmpty()) {
                statusLabel.setText("Please fill number, date and guests");
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





