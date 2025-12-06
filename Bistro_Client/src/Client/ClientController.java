package Client;

import common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Very simple controller for the Bistro client.
 */
public class ClientController {

    // ====== FXML controls ======

    @FXML private TextField txtServerIp;

    @FXML private TextArea txtAreaReservations;

    @FXML private TextField txtReservationNumber;
    @FXML private TextField txtReservationDate;    // yyyy-MM-dd
    @FXML private TextField txtNumberOfGuests;

    @FXML private Label lblStatus;

    @FXML private Button btnConnect;
    @FXML private Button btnDisconnect;
    @FXML private Button btnGetReservations;
    @FXML private Button btnUpdateReservation;

    // ====== client state ======

    private BistroClient client;
    private static final int PORT = 5555;

    // called automatically after FXML loaded
    @FXML
    private void initialize() {
        txtServerIp.setText("127.0.0.1");
        setConnected(false);
        lblStatus.setText("Not connected");
    }

    // enable/disable buttons according to connection state
    private void setConnected(boolean connected) {
        btnConnect.setDisable(connected);
        btnDisconnect.setDisable(!connected);
        btnGetReservations.setDisable(!connected);
        btnUpdateReservation.setDisable(!connected);
    }

    // ====== Connect / Disconnect ======

    @FXML
    private void onConnect() {
        if (client != null && client.isConnected()) {
            return;
        }

        String host = txtServerIp.getText().trim();
        if (host.isEmpty()) host = "127.0.0.1";

        try {
            client = new BistroClient(host, PORT, this);
            client.openConnection();
            lblStatus.setText("Connecting to IP : " + host + " Via Port:" + PORT);
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Connection failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDisconnect() {
        disconnectFromServer();
    }

    public void disconnectFromServer() {
        if (client != null && client.isConnected()) {
            try {
                client.closeConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ====== Buttons: Get / Update ======

    @FXML
    private void onGetReservations() {
        if (!isConnected()) return;

        try {
            Message msg = new Message(Message.Type.GET_RESERVATIONS);
            client.sendToServer(msg);
            lblStatus.setText("Requesting reservations...");
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Error sending request");
        }
    }

    @FXML
    private void onUpdateReservation() {
        if (!isConnected()) return;

        try {
            int num = Integer.parseInt(txtReservationNumber.getText().trim());
            String dateStr = txtReservationDate.getText().trim();
            int guests = Integer.parseInt(txtNumberOfGuests.getText().trim());

            Message msg = new Message(Message.Type.UPDATE_RESERVATION);
            msg.setReservationNumber(num);
            msg.setReservationDate(dateStr);
            msg.setNumberOfGuests(guests);

            client.sendToServer(msg);
            lblStatus.setText("Sending update...");

        } catch (NumberFormatException e) {
            lblStatus.setText("Please enter valid numbers");
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Error sending update");
        }
    }

    private boolean isConnected() {
        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected");
            return false;
        }
        return true;
    }

    // ====== Callbacks from BistroClient ======

    public void onConnected() {
        Platform.runLater(() -> {
            setConnected(true);
            lblStatus.setText("Connected");
        });
    }

    public void onDisconnected() {
        Platform.runLater(() -> {
            setConnected(false);
            lblStatus.setText("Disconnected");
        });
    }

    public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            setConnected(false);
            lblStatus.setText("Connection error : " + e.getMessage());
        });
    }

    // ====== Messages from server ======

    public void handleServerMessage(Object msg) {
        Platform.runLater(() -> {
            if (msg instanceof Message) {
                Message m = (Message) msg;

                switch (m.getType()) {
                    case RESERVATIONS_TEXT:
                        txtAreaReservations.setText(
                                m.getText() != null ? m.getText() : ""
                        );
                        lblStatus.setText("Reservations received");
                        break;

                    case UPDATE_RESULT:
                        lblStatus.setText(
                                m.getText() != null ? m.getText() : "Update result"
                        );
                        break;

                    case ERROR:
                        lblStatus.setText("Error : " + m.getText());
                        break;

                    default:
                        lblStatus.setText("Unknown message type");
                }

            } else {
                // fallback if server sends plain String
                txtAreaReservations.setText(String.valueOf(msg));
                lblStatus.setText("Raw message from server");
            }
        });
    }
}






