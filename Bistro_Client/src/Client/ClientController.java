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

    @FXML private VBox paneNewReservation;
    @FXML private TextField txtNumCustomers;
    @FXML private DatePicker dpReservationDate;
    @FXML private ComboBox<String> cbReservationTime;
    @FXML private Label lblReservationFormMsg;

    // ✅ NEW: suggested alternatives UI
    @FXML private Label lblSuggestedTimesTitle;
    @FXML private ListView<Timestamp> lvSuggestedTimes;
    private final ObservableList<Timestamp> suggestedTimes = FXCollections.observableArrayList();

    // ===== Reservations table =====
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
    
    @FXML private TableColumn<ReservationRow, Number> colHistResId;
    @FXML private TableColumn<ReservationRow, String> colHistConfCode;
    @FXML private TableColumn<ReservationRow, String> colHistResTime;
    @FXML private TableColumn<ReservationRow, String> colHistExpTime;
    @FXML private TableColumn<ReservationRow, Number> colHistCustomers;
    @FXML private TableColumn<ReservationRow, String> colHistStatus;


    @FXML private TableView<ReservationRow> tblHistory;
    private final ObservableList<ReservationRow> history = FXCollections.observableArrayList();
    
    private boolean isSubscriber;



    // IMPORTANT: this field now mirrors the SINGLE shared client from ClientSession
    private BistroClient client;

    // ✅ Pending reservation base (after availability check)
    private MakeReservationRequestDTO pendingReservationBase;


    @FXML
    private void initialize() {
        isSubscriber = "SUBSCRIBER".equals(ClientSession.getRole());

        lblStatus.setText("Ready.");
        lblUserInfo.setText(isSubscriber ? "Welcome, User (Subscriber)" : "Welcome, User (Customer)");

        // sync with already-connected shared client (if exists)
        this.client = ClientSession.getClient();

        btnMyProfile.setVisible(isSubscriber);
        btnMyProfile.setManaged(isSubscriber);

        btnHistory.setVisible(isSubscriber);
        btnHistory.setManaged(isSubscriber);

        lblActiveReservations.setText("0");
        lblBalanceDue.setText("₪0.00");
        lblSubscriptionStatus.setText(isSubscriber ? "Active (10% off)" : "Not Subscribed");

        setupReservationsTable();
        
        setupHistoryTable();
        tblHistory.setItems(history);
        
        tblHistory.setItems(history);

        tblReservations.setItems(reservations);
        reservations.clear();

        btnCancelReservation.setDisable(true);
        tblReservations.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) ->
                btnCancelReservation.setDisable(newV == null)
        );

        // init New Reservation controls
        if (cbReservationTime != null) {
            cbReservationTime.getItems().setAll(
                    "10:00","10:30","11:00","11:30","12:00","12:30",
                    "13:00","13:30","14:00","14:30","15:00","15:30",
                    "16:00","16:30","17:00","17:30","18:00","18:30",
                    "19:00","19:30","20:00","20:30","21:00","21:30","16:59"
            );
            cbReservationTime.getSelectionModel().select("18:00");
        }

        if (dpReservationDate != null) {
            dpReservationDate.setValue(LocalDate.now());
        }

        if (lblReservationFormMsg != null) {
            lblReservationFormMsg.setText("");
        }

        // ✅ Setup suggested times list (click -> autofill)
        if (lvSuggestedTimes != null) {
            lvSuggestedTimes.setItems(suggestedTimes);

            // Display Timestamp nicely
            lvSuggestedTimes.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Timestamp item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        return;
                    }
                    LocalDateTime ldt = item.toLocalDateTime();
                    setText(ldt.toLocalDate() + "  " + String.format("%02d:%02d", ldt.getHour(), ldt.getMinute()));
                }
            });

            // On click selection -> fill fields
            lvSuggestedTimes.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) {
                    applySuggestedTime(newV);
                }
            });

            hideSuggestedTimesUI();
        }

        showPane(paneDashboard);

        // Auto-load reservations when main screen opens (first login)
        Platform.runLater(() -> {
            if (client != null && client.isConnected()) {
                onRefreshReservations();
            }
        });
    }

    private void setupReservationsTable() {
        colReservationId.setCellValueFactory(c -> c.getValue().reservationIdProperty());
        colConfirmationCode.setCellValueFactory(c -> c.getValue().confirmationCodeProperty());
        colReservationTime.setCellValueFactory(c -> c.getValue().reservationTimeProperty());
        colExpiryTime.setCellValueFactory(c -> c.getValue().expiryTimeProperty());
        colCustomers.setCellValueFactory(c -> c.getValue().numOfCustomersProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
    }
    
    private void setupHistoryTable() {
    	colHistResId.setCellValueFactory(c -> c.getValue().reservationIdProperty());
    	colHistConfCode.setCellValueFactory(c -> c.getValue().confirmationCodeProperty());
    	colHistResTime.setCellValueFactory(c -> c.getValue().reservationTimeProperty());
    	colHistExpTime.setCellValueFactory(c -> c.getValue().expiryTimeProperty());
        colHistCustomers.setCellValueFactory(c -> c.getValue().numOfCustomersProperty());
        colHistStatus.setCellValueFactory(c -> c.getValue().statusProperty());
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

    // ===== Suggested times helpers =====

    private void showSuggestedTimesUI(List<Timestamp> alts) {
        if (lblSuggestedTimesTitle == null || lvSuggestedTimes == null) return;

        suggestedTimes.clear();
        if (alts != null) suggestedTimes.addAll(alts);

        boolean has = !suggestedTimes.isEmpty();

        lblSuggestedTimesTitle.setVisible(has);
        lblSuggestedTimesTitle.setManaged(has);

        lvSuggestedTimes.setVisible(has);
        lvSuggestedTimes.setManaged(has);

        if (has) {
            lvSuggestedTimes.getSelectionModel().clearSelection();
        }
    }

    private void hideSuggestedTimesUI() {
        if (lblSuggestedTimesTitle != null) {
            lblSuggestedTimesTitle.setVisible(false);
            lblSuggestedTimesTitle.setManaged(false);
        }
        if (lvSuggestedTimes != null) {
            lvSuggestedTimes.setVisible(false);
            lvSuggestedTimes.setManaged(false);
        }
        suggestedTimes.clear();
    }

    private void applySuggestedTime(Timestamp ts) {
        LocalDateTime ldt = ts.toLocalDateTime();

        if (dpReservationDate != null) {
            dpReservationDate.setValue(ldt.toLocalDate());
        }
        if (cbReservationTime != null) {
            String hhmm = String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());
            cbReservationTime.getSelectionModel().select(hhmm);
        }

        if (lblReservationFormMsg != null) {
            lblReservationFormMsg.setText("✅ Selected alternative time. Click 'Create Reservation' again to confirm.");
        }
    }

    // ===== Networking =====

    public void connectToServer(String host, int port) {
        try {
            ClientSession.configure(host, port);
            ClientSession.connect(this);

            this.client = ClientSession.getClient();

            lblStatus.setText("Connecting to " + host + ":" + port + " ...");
        } catch (Exception e) {
            lblStatus.setText("Connection failed: " + e.getMessage());
        }
    }

    public void disconnectFromServer() {
        try {
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
                case RESPONSE_CHECK_AVAILABILITY -> handleAvailabilityCheckResponse(env.getPayload());

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
    
 // ===== Guest contact dialog (email + phone) =====
    private static class GuestContact {
        final String email;
        final String phone;

        GuestContact(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }
    }

    private GuestContact askGuestEmailAndPhone() {

        Dialog<GuestContact> dialog = new Dialog<>();
        dialog.setTitle("Customer Reservation");
        dialog.setHeaderText("Enter your email and phone to receive confirmation + reminders");

        ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com");

        TextField phoneField = new TextField();
        phoneField.setPromptText("05XXXXXXXX");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == okBtn) {
                String email = emailField.getText() == null ? "" : emailField.getText().trim();
                String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
                return new GuestContact(email, phone);
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    
    private void handleAvailabilityCheckResponse(Object payload) {
        if (!(payload instanceof MakeReservationResponseDTO res)) {
            lblStatus.setText("Bad payload for availability check.");
            return;
        }

        if (res.isOk()) {
            // Available -> now continue and ask details (guest only) and send make reservation
            boolean isSubscriber = "SUBSCRIBER".equals(ClientSession.getRole());

            String subscriberUsername = null;
            String guestEmail = null;
            String guestPhone = null;

            if (isSubscriber) {
                subscriberUsername = ClientSession.getUsername();
                if (subscriberUsername == null || subscriberUsername.isBlank()) {
                    if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Missing subscriber username (session).");
                    return;
                }
            } else {
                GuestContact contact = askGuestEmailAndPhone();
                if (contact == null) {
                    if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Cancelled.");
                    return;
                }
                guestEmail = contact.email;
                guestPhone = contact.phone;

                if (guestEmail.isBlank()) {
                    if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Email is required for customers.");
                    return;
                }
                if (guestPhone.isBlank()) {
                    if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Phone is required for customers.");
                    return;
                }
                
                ClientSession.setGuestEmail(guestEmail);
                ClientSession.setGuestPhone(guestPhone);
            }

            // Use the pending base request time+num
            MakeReservationRequestDTO dto = new MakeReservationRequestDTO(
                    subscriberUsername,
                    guestPhone,
                    guestEmail,
                    pendingReservationBase.getNumOfCustomers(),
                    pendingReservationBase.getReservationTime()
            );

            try {
                Envelope env = Envelope.request(OpCode.REQUEST_MAKE_RESERVATION, dto);
                client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));
                if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Sending...");
            } catch (Exception ex) {
                if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Failed: " + ex.getMessage());
            }

        } else {
            // Not available -> show alternatives (no email/phone asked)
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText(res.getMessage());

            List<Timestamp> alts = res.getSuggestedTimes();
            if (alts != null && !alts.isEmpty()) showSuggestedTimesUI(alts);
            else hideSuggestedTimesUI();
        }
    }


    private void handleMakeReservationResponse(Object payload) {
        if (!(payload instanceof MakeReservationResponseDTO res)) {
            lblStatus.setText("Bad payload for make reservation.");
            return;
        }

        if (res.isOk()) {
            lblStatus.setText("✅ " + res.getMessage() + " | Code: " + res.getConfirmationCode());

            // clear alternatives when success
            hideSuggestedTimesUI();

            if (lblReservationFormMsg != null) {
                lblReservationFormMsg.setText("");
            }

            onRefreshReservations();
        } else {
            lblStatus.setText("❌ " + res.getMessage());

            List<Timestamp> alts = res.getSuggestedTimes();
            if (alts != null && !alts.isEmpty()) {
                if (lblReservationFormMsg != null) {
                    lblReservationFormMsg.setText("No availability at the selected time. Pick an alternative below:");
                }
                showSuggestedTimesUI(alts);
            } else {
                if (lblReservationFormMsg != null) {
                    lblReservationFormMsg.setText("No availability and no alternative times found.");
                }
                hideSuggestedTimesUI();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleReservationsResponse(Object payload) {
        if (!(payload instanceof List<?> list)) {
            lblStatus.setText("Bad reservations payload.");
            return;
        }

        reservations.clear();
        history.clear(); // ✅ add this

        int activeCount = 0;
        int totalCount = 0;
        int historyCount = 0; // optional counter

        for (Object dto : list) {
            ReservationRow row = dtoToRow(dto);
            if (row == null) continue;

            totalCount++;

            String status = row.getStatus();
            boolean active = isActiveStatus(status);

            if (active) activeCount++;

            if (isSubscriber) {
                if (active) {
                    // ✅ Active -> My Reservations
                    reservations.add(row);
                } else {
                    // ✅ Inactive -> History
                    history.add(row);
                    historyCount++;
                }
            } else {
                // customer/guest: show all in My Reservations (no History page)
                reservations.add(row);
            }
        }

        // update counters/labels
        lblActiveReservations.setText(String.valueOf(isSubscriber ? activeCount : totalCount));
        lblStatus.setText("Loaded reservations: " + reservations.size() +
                (isSubscriber ? (" | history: " + history.size()) : ""));
    }


    private boolean isActiveStatus(String status) {
        if (status == null) return false;

        String s = status.trim().toUpperCase();

        // Active reservations = still relevant "now"
        // CONFIRMED -> future upcoming
        // ARRIVED -> already came, still active until finished/closed
        // (add more if your DB uses them)
        return s.equals("CONFIRMED") || s.equals("ARRIVED");
    }

    
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
        this.client = ClientSession.getClient();

        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected.");
            return;
        }

        try {
            // If customer and guest identity not known yet, ask now (covers clicking Refresh directly)
            if (!isSubscriber) {
                String email = ClientSession.getGuestEmail();
                String phone = ClientSession.getGuestPhone();

                if (email == null || email.isBlank() || phone == null || phone.isBlank()) {
                    GuestContact contact = askGuestEmailAndPhone();
                    if (contact == null) {
                        lblStatus.setText("Cancelled.");
                        return;
                    }
                    if (contact.email == null || contact.email.isBlank()
                            || contact.phone == null || contact.phone.isBlank()) {
                        lblStatus.setText("Email + phone are required to view your reservations.");
                        return;
                    }
                    ClientSession.setGuestEmail(contact.email.trim());
                    ClientSession.setGuestPhone(contact.phone.trim());
                }
            }

            String role = ClientSession.getRole();
            String username = ClientSession.getUsername();
            String email = ClientSession.getGuestEmail();
            String phone = ClientSession.getGuestPhone();

            Object payload = new Object[] { role, username, email, phone };

            Envelope env = Envelope.request(OpCode.REQUEST_RESERVATIONS_LIST, payload);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblStatus.setText("Refreshing reservations...");
        } catch (Exception e) {
            lblStatus.setText("Send failed: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelReservation() {
        try {
            this.client = ClientSession.getClient();

            if (client == null || !client.isConnected()) {
                lblStatus.setText("Not connected.");
                return;
            }

            ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
            if (r == null) {
                lblStatus.setText("Select a reservation first.");
                return;
            }

            // Only allow canceling active ones from UI side (server will re-check anyway)
            String st = r.getStatus();
            if (st != null && (st.equalsIgnoreCase("CANCELED") || st.equalsIgnoreCase("EXPIRED"))) {
                lblStatus.setText("This reservation cannot be cancelled.");
                return;
            }

            String role = ClientSession.getRole();              // "SUBSCRIBER" or "CUSTOMER"
            String username = ClientSession.getUsername();      // subscriber only
            String guestEmail = ClientSession.getGuestEmail();  // customer only (can be null)
            String guestPhone = ClientSession.getGuestPhone();  // customer only (can be null)

            // If customer and no identity stored yet, ask now (same logic you used before)
            if (!"SUBSCRIBER".equalsIgnoreCase(role)) {
                if (guestEmail == null || guestEmail.isBlank() || guestPhone == null || guestPhone.isBlank()) {
                    GuestContact contact = askGuestEmailAndPhone();
                    if (contact == null) {
                        lblStatus.setText("Cancelled.");
                        return;
                    }
                    if (contact.email == null || contact.email.isBlank() || contact.phone == null || contact.phone.isBlank()) {
                        lblStatus.setText("Email + phone are required.");
                        return;
                    }
                    ClientSession.setGuestEmail(contact.email.trim());
                    ClientSession.setGuestPhone(contact.phone.trim());
                    guestEmail = contact.email.trim();
                    guestPhone = contact.phone.trim();
                }
            }

            int reservationId = r.getReservationId();

            Object[] payload = new Object[] {
                    role,
                    username,
                    guestEmail,
                    guestPhone,
                    reservationId
            };

            Envelope env = Envelope.request(OpCode.REQUEST_CANCEL_RESERVATION, payload);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblStatus.setText("Cancelling reservation...");

        } catch (Exception ex) {
            lblStatus.setText("Cancel failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    @FXML private void onPayBill() { SceneManager.showPayBill(); }
    @FXML private void onGoToTerminal() { SceneManager.showTerminal(); }

    @FXML private void onNavDashboard() { showPane(paneDashboard); }
    @FXML
    private void onNavReservations() {
        // "My Reservations" -> go to Dashboard pane (where the table is) and refresh data
        showPane(paneDashboard);

        // If guest/customer: ask for email+phone if not known yet (after logout / first entry)
        if (!isSubscriber) {
            String email = ClientSession.getGuestEmail();
            String phone = ClientSession.getGuestPhone();

            if (email == null || email.isBlank() || phone == null || phone.isBlank()) {
                GuestContact contact = askGuestEmailAndPhone();
                if (contact == null) {
                    lblStatus.setText("Cancelled.");
                    return;
                }
                if (contact.email == null || contact.email.isBlank()
                        || contact.phone == null || contact.phone.isBlank()) {
                    lblStatus.setText("Email + phone are required to view your reservations.");
                    return;
                }

                ClientSession.setGuestEmail(contact.email.trim());
                ClientSession.setGuestPhone(contact.phone.trim());
            }
        }

        // Finally, fetch the list from the server
        onRefreshReservations();
    }

    @FXML private void onNavProfile() { if (isSubscriber) showPane(paneProfile); }
    @FXML private void onNavHistory() { if (isSubscriber) showPane(paneHistory); }

    @FXML
    private void onLogout(ActionEvent e) {
    	ClientSession.clearGuestIdentity();
        SceneManager.showLogin();
    }

    @FXML
    private void onNewReservation(ActionEvent e) {
        showPane(paneNewReservation);
        lblStatus.setText("Fill the form and click Create Reservation.");

        if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");
        hideSuggestedTimesUI();
    }

    @FXML
    private void onViewReservationDetails(ActionEvent e) {
        ReservationRow r = tblReservations.getSelectionModel().getSelectedItem();
        if (r == null) {
            lblStatus.setText("Select a reservation first.");
            return;
        }
        lblStatus.setText("Selected: " + r.getConfirmationCode());
    }

    @FXML
    private void onSaveProfile(ActionEvent e) {
        lblStatus.setText("Profile saved (todo).");
    }
    ////to refactor
    public BistroClient getClient() {
        return client;
    }
 
    // temp to delete after login is available
    public void setClient(BistroClient client) {
        this.client = client;
    }


    @FXML
    private void onCreateReservation(ActionEvent e) {
        try {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");

            this.client = ClientSession.getClient();

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

            // Save pending base request
            pendingReservationBase = new MakeReservationRequestDTO(null, null, null, num, ts);

            // ✅ Ask server if there is availability BEFORE asking for email/phone
            Envelope env = Envelope.request(OpCode.REQUEST_CHECK_AVAILABILITY, pendingReservationBase);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Checking availability...");

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

        hideSuggestedTimesUI();
    }
}