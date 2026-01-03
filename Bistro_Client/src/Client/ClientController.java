package Client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.MakeReservationResponseDTO;
import common.dto.MakeReservationRequestDTO;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.lang.reflect.Method;
import java.util.List;

public class ClientController implements ClientUI {

    @FXML private Label lblStatus;
    @FXML private Label lblUserInfo;

    @FXML private Button btnMyProfile;
    @FXML private Button btnHistory;

    @FXML private VBox paneDashboard;
    @FXML private VBox paneProfile;
    @FXML private VBox paneHistory;

    @FXML private Label lblActiveReservations;
    @FXML private Label lblBalanceDue;
    @FXML private Label lblSubscriptionStatus;

    @FXML private TextField txtRecoverPhoneOrEmail;
    @FXML private Label lblRecoverResult;

    @FXML private VBox paneNewReservation;
    @FXML private TextField txtNumCustomers;
    @FXML private DatePicker dpReservationDate;
    @FXML private ComboBox<String> cbReservationTime;
    @FXML private Label lblReservationFormMsg;

    // ===== Reservations table (UPDATED IDS expected in your FXML) =====
    @FXML private TableView<ReservationRow> tblReservations;

    @FXML private TableColumn<ReservationRow, Number> colReservationId;
    @FXML private TableColumn<ReservationRow, String> colConfirmationCode;
    @FXML private TableColumn<ReservationRow, String> colReservationTime;
    @FXML private TableColumn<ReservationRow, String> colExpiryTime;
    @FXML private TableColumn<ReservationRow, Number> colCustomers;
    @FXML private TableColumn<ReservationRow, String> colStatus;

    @FXML private Button btnCancelReservation;

    private final ObservableList<ReservationRow> reservations = FXCollections.observableArrayList();

    // Profile / history still UI-only for now
    @FXML private TextField txtMemberNumber;
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;

    @FXML private TableView<HistoryRow> tblHistory;
    private final ObservableList<HistoryRow> history = FXCollections.observableArrayList();

    private boolean isSubscriber = true;

    // IMPORTANT: this field now mirrors the SINGLE shared client from ClientSession
    private BistroClient client;

    @FXML
    private void initialize() {
    	isSubscriber = "SUBSCRIBER".equals(ClientSession.getRole());
    	
        lblStatus.setText("Ready.");
        lblUserInfo.setText(isSubscriber ? "Welcome, User (Subscriber)" : "Welcome, User (Customer)");

        // ✅ FIX: when controller is created after switching scenes,
        // sync it with the already-connected shared client (if exists)
        this.client = ClientSession.getClient();

        btnMyProfile.setVisible(isSubscriber);
        btnMyProfile.setManaged(isSubscriber);

        btnHistory.setVisible(isSubscriber);
        btnHistory.setManaged(isSubscriber);

        // No demo money now
        lblActiveReservations.setText("0");
        lblBalanceDue.setText("₪0.00");
        lblSubscriptionStatus.setText(isSubscriber ? "Active (10% off)" : "Not Subscribed");

        setupReservationsTable();

        tblReservations.setItems(reservations);
        reservations.clear();

        btnCancelReservation.setDisable(true);
        tblReservations.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
                btnCancelReservation.setDisable(newV == null)
        );

        // init New Reservation controls (if pane exists in this screen)
        if (cbReservationTime != null) {
            cbReservationTime.getItems().setAll(
                    "10:00","10:30","11:00","11:30","12:00","12:30",
                    "13:00","13:30","14:00","14:30","15:00","15:30",
                    "16:00","16:30","17:00","17:30","18:00","18:30",
                    "19:00","19:30","20:00","20:30","21:00","21:30"
            );
            cbReservationTime.getSelectionModel().select("18:00");
        }

        if (dpReservationDate != null) {
            dpReservationDate.setValue(LocalDate.now());
        }

        if (lblReservationFormMsg != null) {
            lblReservationFormMsg.setText("");
        }

        showPane(paneDashboard);
    }

    private void setupReservationsTable() {
        colReservationId.setCellValueFactory(c -> c.getValue().reservationIdProperty());
        colConfirmationCode.setCellValueFactory(c -> c.getValue().confirmationCodeProperty());
        colReservationTime.setCellValueFactory(c -> c.getValue().reservationTimeProperty());
        colExpiryTime.setCellValueFactory(c -> c.getValue().expiryTimeProperty());
        colCustomers.setCellValueFactory(c -> c.getValue().numOfCustomersProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
    }

    private void showPane(VBox pane) {
        paneDashboard.setVisible(false); paneDashboard.setManaged(false);
        paneProfile.setVisible(false);   paneProfile.setManaged(false);
        paneHistory.setVisible(false);   paneHistory.setManaged(false);

        if (paneNewReservation != null) {
            paneNewReservation.setVisible(false);
            paneNewReservation.setManaged(false);
        }

        pane.setVisible(true);
        pane.setManaged(true);
    }

    // ===== Networking =====

    public void connectToServer(String host, int port) {
        try {
            ClientSession.configure(host, port);
            ClientSession.connect(this);

            // ✅ FIX: always mirror the shared client after connect
            this.client = ClientSession.getClient();

            lblStatus.setText("Connecting to " + host + ":" + port + " ...");
        } catch (Exception e) {
            lblStatus.setText("Connection failed: " + e.getMessage());
        }
    }

    public void disconnectFromServer() {
        try {
            // ✅ FIX: use the shared connection (not only the controller field)
            BistroClient c = ClientSession.getClient();
            this.client = c;

            if (c != null && c.isConnected()) {
                c.closeConnection();
            }
        } catch (Exception ignored) {
        }
    }

    public void onConnected() {
        Platform.runLater(() -> lblStatus.setText("Connected to server."));
    }

    public void onDisconnected() {
        Platform.runLater(() -> lblStatus.setText("Disconnected."));
    }

    public void onConnectionError(Exception e) {
        Platform.runLater(() -> lblStatus.setText("Connection error: " + e.getMessage()));
    }

    public void handleServerMessage(Object msg) {
        Platform.runLater(() -> {
            Envelope env = unwrapToEnvelope(msg);
            if (env == null) {
                lblStatus.setText("Decode failed.");
                return;
            }
            if (!env.isOk()) {
                lblStatus.setText("ERROR: " + env.getMessage());
                return;
            }

            switch (env.getOp()) {
                case RESPONSE_RESERVATIONS_LIST -> handleReservationsResponse(env.getPayload());
                case RESPONSE_MAKE_RESERVATION -> handleMakeReservationResponse(env.getPayload());
                default -> lblStatus.setText("Server replied: " + env.getOp());
            }
        });
    }

    private Envelope unwrapToEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope e) return e;

            if (msg instanceof KryoMessage km) {
                Object obj = KryoUtil.fromBytes(km.getPayload());
                if (obj instanceof Envelope e) return e;
            }
        } catch (Exception ex) {
            lblStatus.setText("Decode error: " + ex.getMessage());
        }
        return null;
    }

    private void handleMakeReservationResponse(Object payload) {
        if (!(payload instanceof MakeReservationResponseDTO res)) {
            lblStatus.setText("Bad payload for make reservation.");
            return;
        }

        if (res.isOk()) {
            lblStatus.setText("✅ " + res.getMessage() +
                    " | Code: " + res.getConfirmationCode());

            // refresh the table after successful reservation
            onRefreshReservations();
        } else {
            lblStatus.setText("❌ " + res.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleReservationsResponse(Object payload) {
        if (!(payload instanceof List<?> list)) {
            lblStatus.setText("Bad reservations payload.");
            return;
        }

        reservations.clear();

        for (Object dto : list) {
            ReservationRow row = dtoToRow(dto);
            if (row != null) reservations.add(row);
        }

        lblActiveReservations.setText(String.valueOf(reservations.size()));
        lblStatus.setText("Loaded reservations: " + reservations.size());
    }

    /**
     * Converts common.dto.ReservationDTO -> ReservationRow using reflection
     * (so client won’t crash if DTO changes slightly).
     */
    private ReservationRow dtoToRow(Object dto) {
        try {
            int id = getInt(dto, "getReservationId", 0);
            String code = getString(dto, "getConfirmationCode", null);
            if (code == null) code = getString(dto, "getCode", "-");

            String resTime = getString(dto, "getReservationTime", "-");
            String expTime = getString(dto, "getExpiryTime", "-");
            int customers = getInt(dto, "getNumOfCustomers", 0);
            String status = getString(dto, "getStatus", "-");

            return new ReservationRow(id, code, resTime, expTime, customers, status);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getString(Object obj, String methodName, String def) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            return (v == null) ? def : v.toString();
        } catch (Exception e) {
            return def;
        }
    }

    private int getInt(Object obj, String methodName, int def) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            if (v == null) return def;
            if (v instanceof Number n) return n.intValue();
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return def;
        }
    }

    // ===== UI actions =====

    @FXML
    private void onRefreshReservations() {
        // ✅ FIX: always pull the shared client right before using it
        this.client = ClientSession.getClient();

        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected.");
            return;
        }

        try {
            Envelope env = Envelope.request(OpCode.REQUEST_RESERVATIONS_LIST, null);
            byte[] bytes = KryoUtil.toBytes(env);

            client.sendToServer(new KryoMessage("ENVELOPE", bytes));

            lblStatus.setText("Refreshing reservations...");
        } catch (Exception e) {
            lblStatus.setText("Send failed: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelReservation() {
        ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
        if (r == null) return;

        // TODO later: send REQUEST_CANCEL_RESERVATION to server
        r.setStatus("CANCELLED");
        tblReservations.refresh();
        lblStatus.setText("Reservation cancelled (UI only for now).");
    }

    @FXML private void onPayBill() { SceneManager.showPayBill(); }
    @FXML private void onGoToTerminal() { SceneManager.showTerminal(); }

    @FXML private void onNavDashboard() { showPane(paneDashboard); }
    @FXML private void onNavReservations() { showPane(paneDashboard); }

    @FXML private void onNavProfile() { if (isSubscriber) showPane(paneProfile); }
    @FXML private void onNavHistory() { if (isSubscriber) showPane(paneHistory); }

    @FXML
    private void onLogout(ActionEvent e) {
    	SceneManager.showLogin();
    }

    @FXML
    private void onNewReservation(ActionEvent e) {
        showPane(paneNewReservation);
        lblStatus.setText("Fill the form and click Create Reservation.");
        if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");
    }

    @FXML
    private void onRecoverCode(ActionEvent e) {
        lblRecoverResult.setText("Recovery (todo).");
        // later: send REQUEST_RECOVER_CONFIRMATION_CODE with phone/email
    }

    @FXML
    private void onViewReservationDetails(ActionEvent e) {
        ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
        if (r == null) {
            lblStatus.setText("Select a reservation first.");
            return;
        }
        lblStatus.setText("Selected: " + r.getConfirmationCode());
        // later: open details dialog / right-side pane
    }

    @FXML
    private void onSaveProfile(ActionEvent e) {
        lblStatus.setText("Profile saved (todo).");
        // later: send REQUEST_PROFILE_UPDATE_CONTACT with phone/email
    }

    @FXML
    private void onCreateReservation(ActionEvent e) {
        try {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");

            // ✅ FIX: always pull the shared client right before using it
            this.client = ClientSession.getClient();

            // must be connected
            if (client == null || !client.isConnected()) {
                if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Not connected to server.");
                return;
            }

            int num = Integer.parseInt(txtNumCustomers.getText().trim());
            LocalDate date = dpReservationDate.getValue();
            String timeStr = cbReservationTime.getValue();

            if (date == null || timeStr == null) {
                if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Please choose date and time.");
                return;
            }

            LocalTime time = LocalTime.parse(timeStr);
            Timestamp ts = Timestamp.valueOf(LocalDateTime.of(date, time));

            // choose username (TEMP fallback until you hook login)
            String username = (txtMemberNumber != null && !txtMemberNumber.getText().isBlank())
                    ? txtMemberNumber.getText().trim()
                    : "demo_subscriber";

            MakeReservationRequestDTO dto = new MakeReservationRequestDTO(
                    username,
                    null, null,
                    num,
                    ts
            );

            Envelope env = Envelope.request(OpCode.REQUEST_MAKE_RESERVATION, dto);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Sending...");

        } catch (NumberFormatException ex) {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Customers must be a number.");
        } catch (Exception ex) {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onClearReservationForm(ActionEvent e) {
        if (txtNumCustomers != null) txtNumCustomers.clear();
        if (dpReservationDate != null) dpReservationDate.setValue(LocalDate.now());
        if (cbReservationTime != null) cbReservationTime.getSelectionModel().select("18:00");
        if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");
    }
}











