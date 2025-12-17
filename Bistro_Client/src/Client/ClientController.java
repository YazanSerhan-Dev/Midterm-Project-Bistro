package Client;

import common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ClientController {

    // ===== Bottom connection controls (kept from prototype) =====
    @FXML private TextField txtServerIp;
    @FXML private Button btnConnect;
    @FXML private Button btnDisconnect;
    @FXML private Label lblStatus;

    private BistroClient client;
    private static final int PORT = 5555;

    // ===== Top bar =====
    @FXML private Label lblUserInfo;

    // ===== Sidebar subscriber buttons =====
    @FXML private Button btnMyProfile;
    @FXML private Button btnHistory;

    // ===== Center panes =====
    @FXML private VBox paneDashboard;
    @FXML private VBox paneProfile;
    @FXML private VBox paneHistory;

    // ===== Dashboard summary =====
    @FXML private Label lblActiveReservations;
    @FXML private Label lblBalanceDue;
    @FXML private Label lblSubscriptionStatus;

    // ===== Recover code =====
    @FXML private TextField txtRecoverPhoneOrEmail;
    @FXML private Label lblRecoverResult;

    // ===== Reservations table =====
    @FXML private TableView<ReservationRow> tblReservations;
    @FXML private TableColumn<ReservationRow, String> colResCode;
    @FXML private TableColumn<ReservationRow, String> colResDate;
    @FXML private TableColumn<ReservationRow, String> colResTime;
    @FXML private TableColumn<ReservationRow, Integer> colResGuests;
    @FXML private TableColumn<ReservationRow, String> colResStatus;
    @FXML private TableColumn<ReservationRow, String> colResPrice;

    @FXML private Button btnCancelReservation;

    private final ObservableList<ReservationRow> reservations = FXCollections.observableArrayList();

    // ===== Profile fields (subscriber) =====
    @FXML private TextField txtMemberNumber;
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;

    // ===== History table (subscriber) =====
    @FXML private TableView<HistoryRow> tblHistory;
    @FXML private TableColumn<HistoryRow, String> colHistDate;
    @FXML private TableColumn<HistoryRow, String> colHistTime;
    @FXML private TableColumn<HistoryRow, String> colHistType;
    @FXML private TableColumn<HistoryRow, String> colHistDetails;
    @FXML private TableColumn<HistoryRow, String> colHistAmount;

    private final ObservableList<HistoryRow> history = FXCollections.observableArrayList();

    // ===== "Session" flags (for now; later come from Login page) =====
    private boolean isSubscriber = true; // TODO: set from login result

    @FXML
    private void initialize() {
        // Default host
        txtServerIp.setText("127.0.0.1");
        setConnected(false);
        lblStatus.setText("Not connected");

        // Top bar dummy
        lblUserInfo.setText(isSubscriber ? "Welcome, User (Subscriber)" : "Welcome, User (Customer)");

        // Enable/disable subscriber-only buttons
        btnMyProfile.setDisable(!isSubscriber);
        btnHistory.setDisable(!isSubscriber);

        // Summary cards (dummy for UI phase)
        lblActiveReservations.setText("2");
        lblBalanceDue.setText("₪120.00");
        lblSubscriptionStatus.setText(isSubscriber ? "Active (10% off)" : "Not Subscribed");

        setupReservationsTable();
        setupHistoryTable();

        // Dummy data (UI only)
        reservations.setAll(
                new ReservationRow("A1B2C3", "2025-12-20", "19:00", 4, "Confirmed", "₪200"),
                new ReservationRow("X9Y8Z7", "2025-12-28", "21:00", 2, "Pending", "₪120")
        );
        tblReservations.setItems(reservations);

        history.setAll(
                new HistoryRow("2025-11-10", "20:00", "Reservation", "Table for 2 - Completed", "₪180"),
                new HistoryRow("2025-10-05", "18:30", "Visit", "Walk-in visit - Completed", "₪90")
        );
        tblHistory.setItems(history);

        btnCancelReservation.setDisable(true);
        tblReservations.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
                btnCancelReservation.setDisable(newV == null)
        );

        // start at dashboard
        showPane(paneDashboard);
    }

    private void setupReservationsTable() {
        colResCode.setCellValueFactory(c -> c.getValue().codeProperty());
        colResDate.setCellValueFactory(c -> c.getValue().dateProperty());
        colResTime.setCellValueFactory(c -> c.getValue().timeProperty());
        colResGuests.setCellValueFactory(c -> c.getValue().guestsProperty().asObject());
        colResStatus.setCellValueFactory(c -> c.getValue().statusProperty());
        colResPrice.setCellValueFactory(c -> c.getValue().priceProperty());
    }

    private void setupHistoryTable() {
        colHistDate.setCellValueFactory(c -> c.getValue().dateProperty());
        colHistTime.setCellValueFactory(c -> c.getValue().timeProperty());
        colHistType.setCellValueFactory(c -> c.getValue().typeProperty());
        colHistDetails.setCellValueFactory(c -> c.getValue().detailsProperty());
        colHistAmount.setCellValueFactory(c -> c.getValue().amountProperty());
    }

    private void showPane(VBox pane) {
        paneDashboard.setVisible(false); paneDashboard.setManaged(false);
        paneProfile.setVisible(false);   paneProfile.setManaged(false);
        paneHistory.setVisible(false);   paneHistory.setManaged(false);

        pane.setVisible(true);
        pane.setManaged(true);
    }

    // ===== Navigation handlers =====
    @FXML private void onNavDashboard() { showPane(paneDashboard); }
    @FXML private void onNavReservations() { showPane(paneDashboard); } // same area (table)
    @FXML private void onNavProfile() {
        if (!isSubscriber) return;
        // dummy load
        txtMemberNumber.setText("123456");
        txtFullName.setText("User Name");
        txtPhone.setText("05X-XXXXXXX");
        txtEmail.setText("user@email.com");
        showPane(paneProfile);
    }
    @FXML private void onNavHistory() {
        if (!isSubscriber) return;
        showPane(paneHistory);
    }

    // ===== Customer actions (UI only placeholders now) =====
    @FXML private void onNewReservation() {
        info("New Reservation", "Open the New Reservation flow/page (not implemented yet).");
    }

    @FXML private void onPayBill() {
        info("Pay Bill", "Navigate to Paying Bill page (page 5) - not wired yet.");
    }

    @FXML private void onRefreshReservations() {
        // Later: sendToServer(GET_CUSTOMER_RESERVATIONS)
        lblStatus.setText("Refreshing reservations (UI demo)...");
    }

    @FXML private void onViewReservationDetails() {
        ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
        if (r == null) {
            info("Details", "Select a reservation first.");
            return;
        }
        info("Reservation Details",
                "Code: " + r.getCode() + "\nDate: " + r.getDate() + " " + r.getTime() +
                        "\nGuests: " + r.getGuests() + "\nStatus: " + r.getStatus());
    }

    @FXML private void onCancelReservation() {
        ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
        if (r == null) return;

        // Later: sendToServer(CANCEL_RESERVATION, code)
        r.setStatus("Cancelled");
        lblStatus.setText("Reservation cancelled (UI demo).");
        tblReservations.refresh();
    }

    @FXML private void onRecoverCode() {
        String key = txtRecoverPhoneOrEmail.getText().trim();
        if (key.isEmpty()) {
            lblRecoverResult.setText("Enter phone/email first.");
            return;
        }
        // Later: sendToServer(RECOVER_CODE, key)
        lblRecoverResult.setText("Your latest confirmation code is: A1B2C3 (demo)");
    }

    @FXML private void onSaveProfile() {
        if (!isSubscriber) return;

        // Only phone/email editable (as spec)
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();

        if (phone.isEmpty() || email.isEmpty()) {
            info("Profile", "Phone and Email cannot be empty.");
            return;
        }

        // Later: sendToServer(UPDATE_PROFILE_CONTACT, phone/email)
        lblStatus.setText("Profile saved (UI demo).");
        showPane(paneDashboard);
    }

    @FXML private void onLogout() {
        // Later: clear session + go to Login page
        info("Logout", "Logout clicked (navigation not wired yet).");
    }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ===== Connection logic (kept, still useful) =====
    private void setConnected(boolean connected) {
        btnConnect.setDisable(connected);
        btnDisconnect.setDisable(!connected);
    }

    @FXML
    private void onConnect() {
        if (client != null && client.isConnected()) return;

        String host = txtServerIp.getText().trim();
        if (host.isEmpty()) host = "127.0.0.1";

        try {
            client = new BistroClient(host, PORT, this);
            client.openConnection();
            lblStatus.setText("Connecting to " + host + ":" + PORT);
        } catch (Exception e) {
            lblStatus.setText("Connection failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDisconnect() {
        disconnectFromServer();
    }

    public void disconnectFromServer() {
        if (client != null && client.isConnected()) {
            try { client.closeConnection(); }
            catch (Exception ignored) {}
        }
    }

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
            lblStatus.setText("Connection error: " + e.getMessage());
        });
    }

    public void handleServerMessage(Object msg) {
        // For now just display status. Later you’ll parse typed responses and update tables.
        Platform.runLater(() -> {
            if (msg instanceof Message m) {
                lblStatus.setText("Server message: " + m.getType());
            } else {
                lblStatus.setText("Server: " + String.valueOf(msg));
            }
        });
    }
}








