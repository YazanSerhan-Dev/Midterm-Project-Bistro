package Client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import common.Envelope;
import common.OpCode;
import common.KryoMessage;
import common.KryoUtil;


public class ClientController {

    // ===== Status / top bar =====
    @FXML private Label lblStatus;
    @FXML private Label lblUserInfo;

    // ===== Subscriber-only buttons =====
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

    // ===== Later comes from Login =====
    private boolean isSubscriber = true; // TODO: set from login result

    // Keep for future networking (not shown in UI)
    private BistroClient client;

    @FXML
    private void initialize() {
        lblStatus.setText("Ready.");

        // Top bar (demo)
        lblUserInfo.setText(isSubscriber ? "Welcome, User (Subscriber)" : "Welcome, User (Customer)");

        // Subscriber-only buttons
        btnMyProfile.setDisable(!isSubscriber);
        btnHistory.setDisable(!isSubscriber);

        // Summary cards (demo)
        lblActiveReservations.setText("2");
        lblBalanceDue.setText("₪120.00");
        lblSubscriptionStatus.setText(isSubscriber ? "Active (10% off)" : "Not Subscribed");

        setupReservationsTable();
        setupHistoryTable();

        // Demo rows (UI-only)
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

        showPane(paneDashboard);
    }
    
    public void connectToServer(String host, int port) {
        try {
            client = new BistroClient(host, port, this);
            client.openConnection();
            lblStatus.setText("Connecting to " + host + ":" + port + " ...");
        } catch (Exception e) {
            lblStatus.setText("Connection failed: " + e.getMessage());
        }
    }

    public void disconnectFromServer() {
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();
            }
        } catch (Exception ignored) {}
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

    // ===== Navigation =====
    @FXML private void onNavDashboard()   { showPane(paneDashboard); }
    @FXML private void onNavReservations(){ showPane(paneDashboard); }

    @FXML
    private void onNavProfile() {
        if (!isSubscriber) return;

        // demo load
        txtMemberNumber.setText("123456");
        txtFullName.setText("User Name");
        txtPhone.setText("05X-XXXXXXX");
        txtEmail.setText("user@email.com");

        showPane(paneProfile);
        lblStatus.setText("Editing profile (demo).");
    }

    @FXML
    private void onNavHistory() {
        if (!isSubscriber) return;
        showPane(paneHistory);
        lblStatus.setText("Viewing history (demo).");
    }

    // ===== Story actions (UI placeholders for now) =====
    @FXML
    private void onNewReservation() {
        info("New Reservation",
                "This will open the reservation flow:\n" +
                "• choose date/time (30-min slots)\n" +
                "• guests\n" +
                "• system checks availability and returns confirmation code.");
        lblStatus.setText("New Reservation clicked (demo).");
    }

    @FXML
    private void onPayBill() {
        // Navigate to page 5
        SceneManager.showPayBill();
    }

    @FXML
    private void onGoToTerminal() {
        // Navigate to page 4
        SceneManager.showTerminal();
    }

    @FXML
    private void onRefreshReservations() {
        send(OpCode.REQUEST_RESERVATIONS_LIST, null);
    }

    @FXML
    private void onViewReservationDetails() {
        ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
        if (r == null) {
            info("Details", "Select a reservation first.");
            return;
        }

        info("Reservation Details",
                "Code: " + r.getCode() +
                "\nDate/Time: " + r.getDate() + " " + r.getTime() +
                "\nGuests: " + r.getGuests() +
                "\nStatus: " + r.getStatus() +
                "\nPrice: " + r.getPrice());

        lblStatus.setText("Showing reservation details (demo).");
    }

    @FXML
    private void onCancelReservation() {
        ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
        if (r == null) return;

        // send the reservation code
        send(OpCode.REQUEST_CANCEL_RESERVATION, r.getCode());
    }


    @FXML
    private void onRecoverCode() {
        String key = txtRecoverPhoneOrEmail.getText().trim();
        if (key.isEmpty()) {
            lblRecoverResult.setText("Enter phone/email first.");
            return;
        }

        send(OpCode.REQUEST_RECOVER_CONFIRMATION_CODE, key);
        lblRecoverResult.setText("Request sent...");
    }

    @FXML
    private void onSaveProfile() {
        if (!isSubscriber) return;

        // Only phone/email editable
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();

        if (phone.isEmpty() || email.isEmpty()) {
            info("Profile", "Phone and Email cannot be empty.");
            return;
        }

        // Later: update contact info on server
        lblStatus.setText("Profile saved (demo).");
        showPane(paneDashboard);
    }

    @FXML
    private void onLogout() {
        // If LoginView.fxml exists, this will work; otherwise you can keep it as an alert for now.
        try {
            SceneManager.showLogin();
        } catch (Exception ex) {
            info("Logout", "Logout clicked (login page not wired yet).");
        }
    }

    // ===== Helpers =====
    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    
    // OCSF CallBacks
    
    public void handleServerMessage(Object msg) {
        Platform.runLater(() -> {
            Envelope env = unwrapToEnvelope(msg);
            if (env == null) {
                lblStatus.setText("Server: " + String.valueOf(msg));
                return;
            }

            if (!env.isOk()) {
                lblStatus.setText("ERROR: " + env.getMessage());
                return;
            }

            handleEnvelope(env);
        });
    }


    /**
     * Supports both:
     * 1) server sends Envelope directly
     * 2) server sends KryoMessage (byte[]) and we decode using KryoUtil
     */
    private Envelope unwrapToEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope e) {
                return e;
            }
            if (msg instanceof KryoMessage km) {
                Object obj = KryoUtil.fromBytes(km.getPayload());
                if (obj instanceof Envelope e) return e;
            }
        } catch (Exception ex) {
            // keep UI alive even if decode fails
            lblStatus.setText("Decode error: " + ex.getMessage());
        }
        return null;
    }

    public void onConnected() {
        lblStatus.setText("Connected to server.");
    }
    public void onDisconnected() {
        lblStatus.setText("Disconnected.");
    }
    public void onConnectionError(Exception e) {
        lblStatus.setText("Connection error: " + e.getMessage());
    }

    private void send(OpCode op, Object payload) {
        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected. (Connect first)");
            return;
        }

        try {
            Envelope env = Envelope.request(op, payload);
            // Always send KryoMessage over the network
            client.sendToServer(KryoMessage.of(op.name(), env));
            lblStatus.setText("Sent: " + op);
        } catch (Exception e) {
            lblStatus.setText("Send failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEnvelope(Envelope env) {
        switch (env.getOp()) {
            case RESPONSE_RESERVATIONS_LIST -> {
                // payload should be List<ReservationDTO>
                var list = (java.util.List<common.dto.ReservationDTO>) env.getPayload();

                reservations.clear();
                if (list != null) {
                    for (var dto : list) {
                        reservations.add(new ReservationRow(
                                dto.getCode(),
                                dto.getDate(),
                                dto.getTime(),
                                dto.getGuests(),
                                dto.getStatus(),
                                "₪" + (int) dto.getPrice()
                        ));
                    }
                }
                tblReservations.refresh();
                lblStatus.setText("Loaded reservations: " + (list == null ? 0 : list.size()));
            }

            case RESPONSE_HISTORY_GET -> {
                var list = (java.util.List<common.dto.HistoryDTO>) env.getPayload();

                history.clear();
                if (list != null) {
                    for (var dto : list) {
                        history.add(new HistoryRow(
                                dto.getDate(),
                                dto.getTime(),
                                dto.getType(),
                                dto.getDetails(),
                                "₪" + (int) dto.getAmount()
                        ));
                    }
                }
                tblHistory.refresh();
                lblStatus.setText("Loaded history: " + (list == null ? 0 : list.size()));
            }

            case INFO -> lblStatus.setText(env.getMessage() != null ? env.getMessage() : "INFO");
            case ERROR -> lblStatus.setText("ERROR: " + env.getMessage());

            default -> lblStatus.setText("Unhandled response: " + env.getOp());
        }
    }

}









