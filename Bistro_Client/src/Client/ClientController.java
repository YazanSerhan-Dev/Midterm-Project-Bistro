package Client;

import common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * JavaFX controller for the Bistro client window.
 * Manages connection to the server and reservation actions.
 */
public class ClientController {

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

    private BistroClient client;
    private static final int PORT = 5555;

    /**
     * Initializes the UI with default values and disconnected state.
     */
    @FXML
    private void initialize() {
        txtServerIp.setText("127.0.0.1");
        setConnected(false);
        lblStatus.setText("Not connected");
    }

    /**
     * Enables or disables UI buttons according to connection state.
     */
    private void setConnected(boolean connected) {
        btnConnect.setDisable(connected);
        btnDisconnect.setDisable(!connected);
        btnGetReservations.setDisable(!connected);
        btnUpdateReservation.setDisable(!connected);
    }

    /**
     * Handles Connect button click: creates client and opens connection.
     */
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

    /**
     * Handles Disconnect button click: closes the connection if active.
     */
    @FXML
    private void onDisconnect() {
        disconnectFromServer();
    }

    /**
     * Closes the client connection safely if it is currently connected.
     */
    public void disconnectFromServer() {
        if (client != null && client.isConnected()) {
            try {
                client.closeConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a request to the server to fetch all reservations.
     */
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

    /**
     * Sends an update request for a specific reservation (date & guests).
     */
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

    /**
     * Checks if the client is currently connected; updates status label if not.
     */
    private boolean isConnected() {
        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected");
            return false;
        }
        return true;
    }

    /**
     * Callback from BistroClient when a connection is successfully established.
     */
    public void onConnected() {
        Platform.runLater(() -> {
            setConnected(true);
            lblStatus.setText("Connected");
        });
    }

    /**
     * Callback from BistroClient when the connection is closed.
     */
    public void onDisconnected() {
        Platform.runLater(() -> {
            setConnected(false);
            lblStatus.setText("Disconnected");
        });
    }

    /**
     * Callback from BistroClient when a connection error occurs.
     */
    public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            setConnected(false);
            lblStatus.setText("Connection error : " + e.getMessage());
        });
    }

    /**
     * Handles messages received from the server and updates the UI accordingly.
     */
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

                    default:
                        lblStatus.setText("Unknown message type");
                }

            } else {
                txtAreaReservations.setText(String.valueOf(msg));
                lblStatus.setText("Raw message from server");
            }
        });
    }
}







