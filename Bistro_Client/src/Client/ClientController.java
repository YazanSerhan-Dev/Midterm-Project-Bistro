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
import common.dto.ProfileDTO;
import common.dto.WaitingListDTO;
import common.dto.MakeReservationRequestDTO;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.lang.reflect.Method;
import java.util.List;
/**
 * JavaFX controller for the Bistro client application.
 * <p>
 * Manages the main UI navigation (dashboard/profile/history), reservation creation and cancellation,
 * and communication with the server using {@link BistroClient}.
 * </p>
 * <p>
 * Implements {@link ClientUI} to receive connection events and server messages.
 * </p>
 */

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
    @FXML private TextField txtBarcodeData;
    
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

    /**
     * Initializes the controller after FXML loading.
     * Sets initial UI state, table bindings, listeners, and triggers initial data loading when connected.
     */

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

        if (dpReservationDate != null) {
            dpReservationDate.setValue(LocalDate.now());

            dpReservationDate.valueProperty().addListener((obs, oldV, newV) -> {
                hideSuggestedTimesUI();
                requestAvailableTimesForSelectedDate();
            });
        }

        if (cbReservationTime != null) {
            cbReservationTime.getItems().clear();
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
        Platform.runLater(this::requestAvailableTimesForSelectedDate);
    }
    /**
     * Configures the "My Reservations" table column bindings.
     */

    private void setupReservationsTable() {
        colReservationId.setCellValueFactory(c -> c.getValue().reservationIdProperty());
        colConfirmationCode.setCellValueFactory(c -> c.getValue().confirmationCodeProperty());
        colReservationTime.setCellValueFactory(c -> c.getValue().reservationTimeProperty());
        colExpiryTime.setCellValueFactory(c -> c.getValue().expiryTimeProperty());
        colCustomers.setCellValueFactory(c -> c.getValue().numOfCustomersProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
    }
    /**
     * Configures the "History" table column bindings.
     */

    private void setupHistoryTable() {
    	colHistResId.setCellValueFactory(c -> c.getValue().reservationIdProperty());
    	colHistConfCode.setCellValueFactory(c -> c.getValue().confirmationCodeProperty());
    	colHistResTime.setCellValueFactory(c -> c.getValue().reservationTimeProperty());
    	colHistExpTime.setCellValueFactory(c -> c.getValue().expiryTimeProperty());
        colHistCustomers.setCellValueFactory(c -> c.getValue().numOfCustomersProperty());
        colHistStatus.setCellValueFactory(c -> c.getValue().statusProperty());
    }

    /**
     * Displays a single pane and hides all other main panes.
     *
     * @param pane the pane to show
     */

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
    /**
     * Displays the suggested alternative reservation times list in the UI.
     *
     * @param alts list of alternative timestamps suggested by the server (may be null/empty)
     */

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
    /**
     * Hides the suggested times UI and clears any displayed alternatives.
     */

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
    /**
     * Applies a selected suggested time into the reservation form fields (date + time),
     * and shows a hint message to the user.
     *
     * @param ts the suggested reservation time selected from the alternatives list
     */

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
    /**
     * Configures and connects the shared client session to the server.
     *
     * @param host server IP/hostname
     * @param port server port
     */

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
    /**
     * Disconnects the current shared client session from the server if connected.
     */

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
    /**
     * UI callback invoked when the client connection is established successfully.
     */

    public void onConnected() {
        Platform.runLater(() -> lblStatus.setText("Connected to server."));
   
    }
    /**
     * UI callback invoked when the client connection is closed.
     */

    public void onDisconnected() {
        Platform.runLater(() -> lblStatus.setText("Disconnected."));
    }
    /**
     * UI callback invoked when a connection error occurs.
     *
     * @param e the exception describing the connection error
     */

    public void onConnectionError(Exception e) {
        Platform.runLater(() -> lblStatus.setText("Connection error: " + e.getMessage()));
    }
    /**
     * Handles raw messages received from the server, decodes them into {@link Envelope},
     * and dispatches them to the correct response handler.
     *
     * @param msg raw message object received from the server
     */

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
                case RESPONSE_LEAVE_WAITING_LIST -> handleLeaveWaitingListResponse(env.getPayload());
                
                case RESPONSE_GET_PROFILE -> handleGetProfileResponse(env.getPayload());
                case RESPONSE_UPDATE_PROFILE -> handleUpdateProfileResponse(env.getPayload());
                case RESPONSE_GET_AVAILABLE_TIMES -> handleAvailableTimesResponse(env.getPayload());

                default -> lblStatus.setText("Server replied: " + env.getOp());
            }
        });
    }
    /**
     * Attempts to decode a server message into an {@link Envelope}.
     * Supports direct Envelope messages and Kryo-wrapped envelopes.
     *
     * @param msg the raw server message
     * @return decoded Envelope instance, or null if decoding failed
     */

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
    /**
     * Simple container for guest/customer contact details required for confirmations and reminders.
     */

    private static class GuestContact {
        final String email;
        final String phone;

        GuestContact(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }
    }
    /**
     * Validates email format using a basic regex rule.
     *
     * @param email input email string
     * @return true if the email is non-null and matches the expected format, otherwise false
     */

    private static boolean isValidEmailFormat(String email) {
        if (email == null) return false;
        String e = email.trim();
        return e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    /**
     * Validates Israeli phone format starting with 05 and containing 10 digits total.
     *
     * @param phone input phone string
     * @return true if the phone is non-null and matches 05XXXXXXXX, otherwise false
     */

    private static boolean isValidPhone10Digits(String phone) {
        if (phone == null) return false;
        String p = phone.trim();
        return p.matches("^05\\d{8}$");
    }
    /**
     * Opens a dialog asking a guest/customer for email and phone.
     * Input is validated before allowing confirmation.
     *
     * @return {@link GuestContact} containing validated email and phone, or null if cancelled
     */

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
        phoneField.setPromptText("05XXXXXXXX"); // user can type anything, but we validate 10 digits

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(lblError, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Disable OK until valid
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okBtn);
        okButton.setDisable(true);
        Runnable validate = () -> {
            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();

            boolean okEmail = isValidEmailFormat(email);
            boolean okPhone = isValidPhone10Digits(phone);

            if (!email.isBlank() && !okEmail) {
                lblError.setText("Invalid email format.");
            } else if (!phone.isBlank() && !okPhone) {
                lblError.setText("Invalid phone (must be 10 digits like 05XXXXXXXX).");
            } else {
                lblError.setText("");
            }

            okButton.setDisable(!(okEmail && okPhone));
        };

        emailField.textProperty().addListener((obs, oldV, newV) -> validate.run());
        phoneField.textProperty().addListener((obs, oldV, newV) -> validate.run());
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn == okBtn) {
                String email = emailField.getText() == null ? "" : emailField.getText().trim();
                String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
                // safety: validate again
                if (!isValidEmailFormat(email) || !isValidPhone10Digits(phone)) return null;
                return new GuestContact(email, phone);
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }
    /**
     * Handles available reservation times response and populates the time ComboBox.
     *
     * @param payload expected to be a list of available times (as strings)
     */

    @SuppressWarnings("unchecked")
    private void handleAvailableTimesResponse(Object payload) {
        if (cbReservationTime == null) return;

        cbReservationTime.setPromptText("Select time");

        // ✅ IMPORTANT: reset selection/value so it doesn't stick to old value (like 19:30)
        cbReservationTime.getSelectionModel().clearSelection();
        cbReservationTime.setValue(null);
        cbReservationTime.getItems().clear();

        if (!(payload instanceof java.util.List<?> list)) {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Bad times payload.");
            return;
        }

        for (Object o : list) {
            if (o != null) cbReservationTime.getItems().add(o.toString());
        }

        if (cbReservationTime.getItems().isEmpty()) {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Restaurant is closed on selected date.");
        } else {
            cbReservationTime.getSelectionModel().selectFirst(); // ✅ now it will be 09:00/whatever first
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");
        }
    }


    /**
     * Handles profile data response and fills the profile form fields.
     *
     * @param payload expected to be {@link ProfileDTO}
     */

    private void handleGetProfileResponse(Object payload) {
        if (!(payload instanceof ProfileDTO dto)) {
            lblStatus.setText("Bad profile payload.");
            return;
        }

        txtMemberNumber.setText(dto.getMemberNumber());
        txtFullName.setText(dto.getFullName());
        txtPhone.setText(dto.getPhone() == null ? "" : dto.getPhone());
        txtEmail.setText(dto.getEmail() == null ? "" : dto.getEmail());

        // ✅ NEW
        if (txtBarcodeData != null) {
            txtBarcodeData.setText(dto.getBarcodeData() == null ? "" : dto.getBarcodeData());
            txtBarcodeData.setEditable(false);
        }

        lblStatus.setText("Profile loaded.");
    }


    /**
     * Handles profile update response. Payload may be a message string or an updated {@link ProfileDTO}.
     *
     * @param payload server response payload
     */

    private void handleUpdateProfileResponse(Object payload) {
        // simplest: server returns String message OR ProfileDTO back
        if (payload instanceof String msg) {
            lblStatus.setText(msg);
            return;
        }

        if (payload instanceof common.dto.ProfileDTO dto) {
            txtFullName.setText(dto.getFullName() == null ? "" : dto.getFullName());
            txtPhone.setText(dto.getPhone() == null ? "" : dto.getPhone());
            txtEmail.setText(dto.getEmail() == null ? "" : dto.getEmail());
            lblStatus.setText("✅ Profile updated.");
            return;
        }

        lblStatus.setText("✅ Profile updated.");
    }

    /**
     * Handles availability check response before making a reservation.
     * If available, continues to collect user details (guest only) and sends the make-reservation request.
     * If not available, shows alternative suggested times.
     *
     * @param payload expected to be {@link MakeReservationResponseDTO}
     */

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

    /**
     * Handles make-reservation response and updates the UI accordingly.
     *
     * @param payload expected to be {@link MakeReservationResponseDTO}
     */

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
    /**
     * Handles reservations list response and updates the dashboard tables and counters.
     * Subscribers split reservations into active vs history.
     *
     * @param payload expected to be a list of reservation DTO objects
     */

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

    /**
     * Determines whether a reservation status is considered active for display purposes.
     *
     * @param status reservation status text
     * @return true if status represents an active reservation, otherwise false
     */

    private boolean isActiveStatus(String status) {
        if (status == null) return false;

        String s = status.trim().toUpperCase();

        // Active reservations = still relevant "now"
        // CONFIRMED -> future upcoming
        // ARRIVED -> already came, still active until finished/closed
        // (add more if your DB uses them)
        return s.equals("CONFIRMED") || s.equals("ARRIVED") || s.equals("PENDING");
    }

    /**
     * Converts a reservation DTO (unknown concrete type) into a {@link ReservationRow} using reflection.
     *
     * @param dto reservation DTO object returned by the server
     * @return a populated {@link ReservationRow}, or null if conversion failed
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
    /**
     * Invokes a no-arg getter method by reflection and returns its string form.
     *
     * @param obj target object
     * @param methodName getter method name to call
     * @param def default value used when invocation fails or returns null
     * @return the extracted string value, or the default value
     */

    private String getString(Object obj, String methodName, String def) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            return (v == null) ? def : v.toString();
        } catch (Exception e) {
            return def;
        }
    }
    /**
     * Invokes a no-arg getter method by reflection and parses the result as an int.
     *
     * @param obj target object
     * @param methodName getter method name to call
     * @param def default value used when invocation fails or returns null
     * @return parsed integer value, or the default value
     */

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
    /**
     * Handles leave-waiting-list response and updates the status message.
     *
     * @param payload expected to be {@link WaitingListDTO} or an error message string
     */

    private void handleLeaveWaitingListResponse(Object payload) {
        if (payload instanceof WaitingListDTO dto) {
            lblStatus.setText("✅ Left waiting list. Code: " + dto.getConfirmationCode() +
                    " | Status: " + dto.getStatus());
            return;
        }

        // If server sends string messages for errors, show them
        lblStatus.setText(payload == null ? "✅ Left waiting list." : payload.toString());
    }
    /**
     * Requests available reservation times for the currently selected date from the server,
     * and updates the time selection UI while loading.
     */

    private void requestAvailableTimesForSelectedDate() {
        this.client = ClientSession.getClient();

        if (client == null || !client.isConnected()) {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Not connected to server.");
            return;
        }

        if (dpReservationDate == null || cbReservationTime == null) return;

        LocalDate date = dpReservationDate.getValue();
        if (date == null) return;

        try {
            Envelope env = Envelope.request(OpCode.REQUEST_GET_AVAILABLE_TIMES, date.toString());
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            cbReservationTime.getItems().clear();
            cbReservationTime.setPromptText("Loading times...");
        } catch (Exception ex) {
            if (lblReservationFormMsg != null) lblReservationFormMsg.setText("Failed to load times: " + ex.getMessage());
        }
    }



    // ===== UI actions =====
    /**
     * UI action: sends a request to leave the waiting list after asking the user for the waiting code.
     */

    @FXML
    private void onLeaveWaitingList() {
        this.client = ClientSession.getClient();

        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected.");
            return;
        }

        // Ask for waiting code (since main page doesn't show it)
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Leave Waiting List");
        dialog.setHeaderText("Enter your waiting list code");
        dialog.setContentText("Waiting Code:");

        String code = dialog.showAndWait().orElse("").trim();

        if (code.isBlank()) {
            lblStatus.setText("Cancelled.");
            return;
        }

        try {
            String role = ClientSession.getRole();
            String username = ClientSession.getUsername();

            Object[] payload = new Object[] { role, username, code };

            Envelope env = Envelope.request(OpCode.REQUEST_LEAVE_WAITING_LIST, payload);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblStatus.setText("Leaving waiting list...");
        } catch (Exception e) {
            lblStatus.setText("Send failed: " + e.getMessage());
        }
    }

    /**
     * UI action: requests the current user's reservations list from the server.
     * Guests/customers may be asked for email and phone if not already stored in the session.
     */

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
    /**
     * UI action: cancels the selected reservation (if allowed) by sending a cancel request to the server.
     */

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
            
            onRefreshReservations();

        } catch (Exception ex) {
            lblStatus.setText("Cancel failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * UI action: navigates to the payment screen.
     */

    @FXML private void onPayBill() { SceneManager.showPayBill(); }
    /**
     * UI action: navigates to the dashboard pane.
     */

    @FXML private void onNavDashboard() { showPane(paneDashboard); }
    /**
     * UI action: navigates to reservations view (dashboard) and refreshes reservations from the server.
     */

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
    /**
     * UI action: navigates to profile pane (subscribers only) and requests profile data from the server.
     */

    @FXML
    private void onNavProfile() {
        if (!isSubscriber) return;

        showPane(paneProfile);

        this.client = ClientSession.getClient();
        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected.");
            return;
        }

        try {
            String memberCode = ClientSession.getMemberCode(); // this should store member_code

            if (memberCode == null || memberCode.isBlank()) {
                lblStatus.setText("Member code missing. Please re-login.");
                return;
            }

            Envelope env = Envelope.request(OpCode.REQUEST_GET_PROFILE, memberCode.trim());
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));
            lblStatus.setText("Loading profile...");
        } catch (Exception ex) {
            lblStatus.setText("Failed to load profile: " + ex.getMessage());
        }

    }
    /**
     * UI action: copies the barcode data to the system clipboard.
     */

    @FXML
    private void onCopyBarcode() {
        if (txtBarcodeData == null) return;

        String code = txtBarcodeData.getText();
        if (code == null || code.isBlank()) {
            lblStatus.setText("No barcode to copy.");
            return;
        }

        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(code);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);

        lblStatus.setText("Barcode copied ✅");
    }
    /**
     * UI action: navigates to the history pane (subscribers only).
     */

    @FXML private void onNavHistory() { if (isSubscriber) showPane(paneHistory); }
    /**
     * UI action: logs out the current user and navigates back to the login screen.
     *
     * @param e action event from the logout button
     */

    @FXML
    private void onLogout(ActionEvent e) {
    	ClientSession.clearGuestIdentity();
        SceneManager.showLogin();
    }
    /**
     * UI action: opens the new reservation form and refreshes available times for the selected date.
     *
     * @param e action event from the "New Reservation" button
     */

    @FXML
    private void onNewReservation(ActionEvent e) {
        showPane(paneNewReservation);
        requestAvailableTimesForSelectedDate();
        lblStatus.setText("Fill the form and click Create Reservation.");

        if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");
        hideSuggestedTimesUI();
    }
    /**
     * UI action: validates profile form fields and sends an update-profile request to the server.
     *
     * @param e action event from the "Save" button
     */

    @FXML
    private void onSaveProfile(ActionEvent e) {
        if (!isSubscriber) return;

        this.client = ClientSession.getClient();
        if (client == null || !client.isConnected()) {
            lblStatus.setText("Not connected.");
            return;
        }

        String memberCode = txtMemberNumber.getText() == null ? "" : txtMemberNumber.getText().trim();
        String fullName   = txtFullName.getText() == null ? "" : txtFullName.getText().trim();
        String phone      = txtPhone.getText() == null ? "" : txtPhone.getText().trim();
        String email      = txtEmail.getText() == null ? "" : txtEmail.getText().trim();

        if (memberCode.isBlank()) { lblStatus.setText("Member code missing."); return; }
        if (fullName.isBlank()) { lblStatus.setText("Full name is required."); return; }
        if (phone.isBlank()) { lblStatus.setText("Phone is required."); return; }
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            lblStatus.setText("Invalid email.");
            return;
        }

        try {
            ProfileDTO dto = new ProfileDTO();
            dto.setMemberNumber(memberCode);   // member_code
            dto.setFullName(fullName);         // name
            dto.setPhone(phone);
            dto.setEmail(email);

            Envelope env = Envelope.request(OpCode.REQUEST_UPDATE_PROFILE, dto);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblStatus.setText("Saving profile...");
        } catch (Exception ex) {
            lblStatus.setText("Save failed: " + ex.getMessage());
        }
    }

    /**
     * Returns the current client instance associated with this controller.
     *
     * @return current {@link BistroClient} reference (may be null)
     */

    ////to refactor
    public BistroClient getClient() {
        return client;
    }
 

    /**
     * UI action: validates reservation form and sends an availability check request to the server.
     * If available, the flow continues in the availability response handler.
     *
     * @param e action event from the "Create Reservation" button
     */

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

    /**
     * UI action: clears the reservation form fields, hides suggested times, and reloads available times.
     *
     * @param e action event from the "Clear" button
     */

    @FXML
    private void onClearReservationForm(ActionEvent e) {
        if (txtNumCustomers != null) txtNumCustomers.clear();
        if (dpReservationDate != null) dpReservationDate.setValue(LocalDate.now());
        if (lblReservationFormMsg != null) lblReservationFormMsg.setText("");

        hideSuggestedTimesUI();
        requestAvailableTimesForSelectedDate();
    }
}