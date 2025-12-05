package Client;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import common.Message;

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

                // 1. נשלח לשרת הודעה שאנחנו רוצים להתנתק
                try {
                    client.sendToServer("CLIENT_QUIT");
                } catch (Exception ignore) {
                    // גם אם זה נכשל, נמשיך לסגור את החיבור
                }

                // 2. נסגור את החיבור בפועל (יגרום ל-connectionClosed בצד הלקוח)
                client.closeConnection();

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

        javafx.application.Platform.runLater(() -> {
            if (msg instanceof Message) {
                Message m = (Message) msg;

                switch (m.getType()) {
                    case RESERVATIONS_TEXT:
                        reservationsArea.setText(m.getText() != null ? m.getText() : "");
                        statusLabel.setText("Reservations loaded");
                        break;

                    case UPDATE_RESULT:
                        if (Boolean.TRUE.equals(m.getSuccess())) {
                            statusLabel.setText("Update succeeded");
                        } else {
                            statusLabel.setText("Update failed: " +
                                    (m.getText() != null ? m.getText() : ""));
                        }
                        break;

                    case INFO:
                        statusLabel.setText("Info: " + (m.getText() != null ? m.getText() : ""));
                        break;

                    case ERROR:
                        statusLabel.setText("Error: " + (m.getText() != null ? m.getText() : ""));
                        break;

                    default:
                        statusLabel.setText("Unknown message type: " + m.getType());
                }

            } else {
                // fallback: if server sends plain String for some reason
                String response = String.valueOf(msg);
                reservationsArea.setText(response);
                statusLabel.setText("Raw message from server");
            }
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

            Message msg = new Message(Message.Type.GET_RESERVATIONS);
            client.sendToServer(msg);
            statusLabel.setText("Requesting reservations...");

        } catch (Exception e) {
            statusLabel.setText("Error sending request: " + e.getMessage());
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

            int num = Integer.parseInt(reservationNumberField.getText().trim());
            String dateStr = dateField.getText().trim();   // yyyy-MM-dd
            int guests = Integer.parseInt(guestsField.getText().trim());

            Message msg = new Message(Message.Type.UPDATE_RESERVATION);
            msg.setReservationNumber(num);
            msg.setReservationDate(dateStr);
            msg.setNumberOfGuests(guests);

            client.sendToServer(msg);
            statusLabel.setText("Sending update request...");

        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid number / guests");
        } catch (Exception e) {
            statusLabel.setText("Error sending update: " + e.getMessage());
            e.printStackTrace();
        }
    }

}





