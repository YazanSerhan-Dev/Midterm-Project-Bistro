package Client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;


import java.lang.reflect.Method;
import java.util.List;

public class ClientController {

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
    private BistroClient client;
    
    private agent.AgentController agentController;
    public void setAgentController(agent.AgentController agentController) {
        this.agentController = agentController;
    }


    @FXML
    private void initialize() {
        lblStatus.setText("Ready.");
        lblUserInfo.setText(isSubscriber ? "Welcome, User (Subscriber)" : "Welcome, User (Customer)");

        btnMyProfile.setDisable(!isSubscriber);
        btnHistory.setDisable(!isSubscriber);

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

        pane.setVisible(true);
        pane.setManaged(true);
    }

    // ===== Networking =====

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
        } catch (Exception ignored) {
        }
    }


    public void onConnected() {
        Platform.runLater(() -> {
            if (lblStatus != null) { // Add this check
                lblStatus.setText("Connected to server.");
            } else {
                System.out.println("Client connected to server.");
            }
        });
    }

    public void onDisconnected() {
        Platform.runLater(() -> {
            if (lblStatus != null) { // Add this check
                lblStatus.setText("Disconnected.");
            } else {
                System.out.println("Client disconnected.");
            }
        });
    }

    public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            if (lblStatus != null) { // Add this check
                lblStatus.setText("Connection error: " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        });
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

                // ===== Customer =====
                case RESPONSE_RESERVATIONS_LIST ->
                        handleReservationsResponse(env.getPayload());
                
                // ======Agent======
                case RESPONSE_SUBSCRIBERS_LIST -> {
                    if (agentController != null) {
                        agentController.updateSubscribersTable((List<?>) env.getPayload());
                    }
                }
                case RESPONSE_AGENT_RESERVATIONS_LIST -> {
                    if (agentController != null) {
                        agentController.updateReservationsTable((List<?>) env.getPayload());
                    }
                }
                


                // ===== Default =====
                default ->
                        lblStatus.setText("Server replied: " + env.getOp());
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
        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected.");
            return;
        }

        try {
            Envelope env = Envelope.request(OpCode.REQUEST_RESERVATIONS_LIST, null);
            byte[] bytes = KryoUtil.toBytes(env);

            // ✅ FIX #1: KryoMessage needs (type, payload)
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
        lblStatus.setText("Logout clicked (demo).");
        disconnectFromServer(); // if you added it
        // later: SceneManager.showLogin();
    }

    @FXML
    private void onNewReservation(ActionEvent e) {
        lblStatus.setText("New reservation clicked (todo).");
        // later: open reservation form pane/dialog
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
    ////to refactor
    public BistroClient getClient() {
        return client;
    }
 
    // temp to delete after login is available
    public void setClient(BistroClient client) {
        this.client = client;
    }
    
    public void send(Envelope env) {
        try {
            client.sendToServer(env);
        } catch (Exception e) {
            onConnectionError(e);
        }
    }



}











