package Client;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.TerminalActiveItemDTO;
import common.dto.TerminalValidateResponseDTO;
import common.dto.WaitingListDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * TerminalController
 * - Uses shared OCSF client from ClientSession
 * - Sends Envelope via KryoMessage
 * - Implements ClientUI so it receives server responses through ClientSession
 */
public class TerminalController implements ClientUI {

    @FXML private Label lblConnectionStatus;

    @FXML private TextField txtConfirmationCode;
    @FXML private Label lblValidationResult;

    // Reservation Details (new UI)
    @FXML private Label lblResId;
    @FXML private Label lblResDateTime;
    @FXML private Label lblResGuests;

    @FXML private Label lblTableNumber;
    @FXML private Label lblWaitMessage;

    @FXML private Button btnCheckIn;
    @FXML private Button btnScanSubscriberQR;
    @FXML private Label lblTerminalStatus;

    // Lost code
    @FXML private TextField txtRecoverPhoneOrEmail;
    @FXML private Label lblRecoverResult;

    @FXML private Button btnJoinWaitingList;
    @FXML private Button btnLeaveWaitingList;

    @FXML private Label lblWaitCode;
    @FXML private Label lblWaitStatus;
    @FXML private Label lblWaitPeople;
    @FXML private Label lblWaitEmail;
    @FXML private Label lblWaitPhone;
    
    @FXML private ListView<TerminalActiveItemDTO> lstSubscriberActive;
    @FXML private VBox subscriberActiveBox;


    // Logic flags (business state)
    private volatile boolean validated = false;
    private volatile boolean tableAvailable = false;

    // UI stays enabled always, but these prevent double-click spam while request is in-flight
    private volatile boolean validateInFlight = false;
    private volatile boolean checkInInFlight  = false;
    private volatile boolean joinWLInFlight   = false;
    private volatile boolean leaveWLInFlight  = false;
    
    private volatile boolean checkedIn = false;
    private volatile boolean cancelResInFlight = false;
    private volatile boolean lastValidatedIsReservation = false;
    private volatile boolean resolveQrInFlight = false;
    private volatile String lastCheckedInCode = "";
    private volatile String lastValidatedCode = "";
    
 // Terminal identity (public terminal session)
    private volatile boolean terminalIsSubscriber = false;
    private volatile String terminalSubscriberUsername = "";


    @FXML
    private void initialize() {
        refreshConnectionLabel();
        resetAll();
        lblTerminalStatus.setText("Ready.");

        // Register this screen as the current UI receiver
        ClientSession.bindUI(this);

        applyRoleVisibility();       
        if (lstSubscriberActive != null) {
            lstSubscriberActive.setOnMouseClicked(e -> {
                var dto = lstSubscriberActive.getSelectionModel().getSelectedItem();
                if (dto == null) return;

                txtConfirmationCode.setText(dto.getConfirmationCode());
                lblTerminalStatus.setText(
                    "Selected " + dto.getType() + ". Validating..."
                );

                onValidateCode(); // 🔥 reuse existing logic
            });
        }
        // Buttons stay enabled always by design (no setDisable calls here)
    }

    // =========================
    // UI Actions
    // =========================
    
    @FXML
    private void onCancelReservation() {

        if (cancelResInFlight) {
            lblTerminalStatus.setText("Cancel request already in progress...");
            return;
        }

        String code = safeTrim(txtConfirmationCode.getText());
        if (code.isEmpty()) {
            lblTerminalStatus.setText("Enter reservation code first.");
            return;
        }

        if (!isConnected()) {
            lblTerminalStatus.setText("Not connected.");
            return;
        }

        // ✅ Strong safety: only allow cancel after validation AND only if it was a reservation
        if (!code.equalsIgnoreCase(lastValidatedCode) || !validated) {
            lblTerminalStatus.setText("Please validate the code first.");
            return;
        }

        if (!lastValidatedIsReservation) {
            lblTerminalStatus.setText("This is not a reservation code.");
            return;
        }

        // Optional: block if already checked-in
        if (checkedIn && code.equalsIgnoreCase(lastCheckedInCode)) {
            lblTerminalStatus.setText("Already checked-in. Cannot cancel.");
            return;
        }

        // Optional confirm dialog
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Cancel Reservation");
        a.setHeaderText("Are you sure you want to cancel this reservation?");
        a.setContentText("Code: " + code);

        ButtonType yes = new ButtonType("Yes, cancel");
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(yes, no);

        if (a.showAndWait().orElse(no) != yes) {
            lblTerminalStatus.setText("Cancelled.");
            return;
        }

        cancelResInFlight = true;
        lblTerminalStatus.setText("Canceling reservation...");

        // ✅ Uses your existing helper pattern
        sendToServer(OpCode.REQUEST_TERMINAL_CANCEL_RESERVATION, code);
    }

    

    @FXML
    private void onJoinWaitingList() {
        if (joinWLInFlight) {
            lblTerminalStatus.setText("Join request already in progress...");
            return;
        }

        if (!isConnected()) {
            lblTerminalStatus.setText("Not connected.");
            return;
        }

        String role = terminalIsSubscriber ? "SUBSCRIBER" : "CUSTOMER";
        String username = terminalIsSubscriber ? terminalSubscriberUsername : "";

        try {
            WaitingListDTO dto = new WaitingListDTO();

            if ("SUBSCRIBER".equalsIgnoreCase(role)) {

                Integer people = askPeopleCountOnly(); // simple dialog
                if (people == null) {
                    lblTerminalStatus.setText("Cancelled.");
                    return;
                }
                dto.setPeopleCount(people);

                // subscriber doesn't enter contact info
                dto.setEmail("");
                dto.setPhone("");

            } else {
                JoinWaitingInput in = askJoinWaitingListData(); // dialog
                if (in == null) {
                    lblTerminalStatus.setText("Cancelled.");
                    return;
                }

                // CUSTOMER: email + phone REQUIRED (your dialog enforces email + people; phone optional)
                dto.setPeopleCount(in.people);
                dto.setEmail(in.email);
                dto.setPhone(in.phone);
            }

            Object[] payload = new Object[] { role, username, dto };

            Envelope env = Envelope.request(OpCode.REQUEST_WAITING_ADD, payload);
            ClientSession.getClient().sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            joinWLInFlight = true;
            lblTerminalStatus.setText("Joining waiting list...");

        } catch (Exception e) {
            joinWLInFlight = false;
            lblTerminalStatus.setText("Failed: " + e.getMessage());
        }
    }

    @FXML
    private void onLeaveWaitingList() {

        if (leaveWLInFlight) {
            lblTerminalStatus.setText("Leave request already in progress...");
            return;
        }

        if (!isConnected()) {
            lblTerminalStatus.setText("Not connected.");
            return;
        }

        // ✅ ALWAYS cancel by the code typed by the user
        String code = (txtConfirmationCode.getText() == null) ? "" : txtConfirmationCode.getText().trim();

        if (code.isEmpty()) {
            lblTerminalStatus.setText("Enter a waiting list code first.");
            return;
        }

        // Optional: if you want to ensure it looks like a waiting code
        // if (!code.startsWith("W")) { ... }  // only if your codes are like W1234

        String role = terminalIsSubscriber ? "SUBSCRIBER" : "CUSTOMER";
        String username = terminalIsSubscriber ? terminalSubscriberUsername : "";

        Object[] payload = new Object[] { role, username, code };

        Envelope env = Envelope.request(OpCode.REQUEST_LEAVE_WAITING_LIST, payload);

        try {
            ClientSession.getClient().sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            leaveWLInFlight = true;
            lblTerminalStatus.setText("Leaving waiting list for code: " + code);

        } catch (Exception e) {
            leaveWLInFlight = false;
            lblTerminalStatus.setText("Failed: " + e.getMessage());
        }
    }


    @FXML
    private void onValidateCode() {
        if (validateInFlight) {
            lblTerminalStatus.setText("Validation already in progress...");
            return;
        }

        String code = safeTrim(txtConfirmationCode.getText());
        lastValidatedCode = code;
        checkedIn = false;
        lastCheckedInCode = "";
        if (code.isEmpty()) {
            setValidationState(false, false, "Enter a code.", "Validation failed: empty code.");
            return;
        }

        if (!isConnected()) {
            setValidationState(false, false, "Not connected.", "Cannot validate: not connected to server.");
            return;
        }

        validateInFlight = true;
        sendToServer(OpCode.REQUEST_TERMINAL_VALIDATE_CODE, code);

        lblValidationResult.setText("Checking...");
        lblTerminalStatus.setText("Validating code...");
        // Buttons remain enabled always (no disabling)
    }

    @FXML
    private void onCheckIn() {

        String code = safeTrim(txtConfirmationCode.getText());
        if (code.isEmpty()) {
            lblTerminalStatus.setText("Check-in blocked: empty code.");
            return;
        }

        // ✅ If already checked-in with same code, don't say "validate first"
        if (checkedIn && code.equalsIgnoreCase(lastCheckedInCode)) {
            lblTerminalStatus.setText("Already checked-in for this code.");
            return;
        }

        // ✅ If code changed since validation, force validation again
        if (!code.equalsIgnoreCase(lastValidatedCode)) {
            validated = false;
            tableAvailable = false;
            lblTerminalStatus.setText("Code changed. Please validate again.");
            return;
        }

        // ✅ Must validate first
        if (!validated) {
            lblTerminalStatus.setText("Check-in blocked: validate code first.");
            return;
        }

        // ✅ Prevent spam clicks while request is in-flight
        if (checkInInFlight) {
            lblTerminalStatus.setText("Check-in already in progress...");
            return;
        }

        if (!isConnected()) {
            lblTerminalStatus.setText("Not connected.");
            return;
        }

        // ✅ Send check-in request to server
        checkInInFlight = true;
        lblTerminalStatus.setText("Checking in...");

        sendToServer(OpCode.REQUEST_TERMINAL_CHECK_IN, code);
    }

    @FXML
    private void onScanSubscriberQR() {

        if (resolveQrInFlight) {
            lblTerminalStatus.setText("QR resolve already in progress...");
            return;
        }

        if (!isConnected()) {
            lblTerminalStatus.setText("Not connected.");
            return;
        }

        // Fake scan dialog: the subscriber types/pastes barcode_data
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Scan Subscriber QR");
        d.setHeaderText("Simulated scan");
        d.setContentText("Paste subscriber barcode_data:");

        d.showAndWait().ifPresent(barcode -> {
            barcode = safeTrim(barcode);
            if (barcode.isEmpty()) {
                lblTerminalStatus.setText("Scan cancelled / empty.");
                return;
            }

            resolveQrInFlight = true;
            lblTerminalStatus.setText("Resolving subscriber QR...");

            // Send barcode_data to server
            sendToServer(OpCode.REQUEST_TERMINAL_RESOLVE_SUBSCRIBER_QR, barcode);
        });
    }

    @FXML
    private void onRecoverCode() {
        String key = safeTrim(txtRecoverPhoneOrEmail.getText());
        if (key.isEmpty()) {
            lblRecoverResult.setText("Enter phone/email first.");
            return;
        }

        if (!isConnected()) {
            lblRecoverResult.setText("Not connected.");
            lblTerminalStatus.setText("Cannot recover: not connected to server.");
            return;
        }

        sendToServer(OpCode.REQUEST_RECOVER_CONFIRMATION_CODE, key);

        lblRecoverResult.setText("Sending recovery request...");
        lblTerminalStatus.setText("Recovering confirmation code...");
    }

    @FXML
    private void onClear() {

        // clear inputs
        if (txtConfirmationCode != null) txtConfirmationCode.clear();
        if (txtRecoverPhoneOrEmail != null) txtRecoverPhoneOrEmail.clear();

        // clear labels/results
        if (lblValidationResult != null) lblValidationResult.setText("");
        if (lblRecoverResult != null) lblRecoverResult.setText("");

        // clear all UI blocks
        resetAll();              // reservation + table + wait message
        resetWaitingDetails();   // waiting list box

        // reset business state
        validated = false;
        tableAvailable = false;
        checkedIn = false;

        lastValidatedIsReservation = false;
        lastValidatedCode = "";
        lastCheckedInCode = "";

        // unlock in-flight flags (safe)
        validateInFlight = false;
        checkInInFlight = false;
        joinWLInFlight = false;
        leaveWLInFlight = false;
        cancelResInFlight = false;

        lblTerminalStatus.setText("Cleared.");
        refreshConnectionLabel();
    }

    // Navigation
    @FXML private void onGoToPayBill() { SceneManager.showPayBill(); }
    @FXML private void onLogout()      { SceneManager.showLogin(); }

    // =========================
    // ClientUI required methods
    // =========================

    @Override
    public void onConnected() {
        Platform.runLater(() -> {
            refreshConnectionLabel();
            lblTerminalStatus.setText("Connected.");
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            refreshConnectionLabel();
            lblTerminalStatus.setText("Disconnected.");
            // Buttons remain enabled always.
            // We also stop any in-flight lock so user can retry after reconnect.
            validateInFlight = false;
            checkInInFlight = false;
            joinWLInFlight = false;
            leaveWLInFlight = false;
            cancelResInFlight = false;
            resolveQrInFlight = false;
        });
    }

    @Override
    public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            refreshConnectionLabel();
            lblTerminalStatus.setText("Connection error: " + (e == null ? "" : e.getMessage()));
            // Buttons remain enabled always.
            // Unlock to allow retry.
            validateInFlight = false;
            checkInInFlight = false;
            joinWLInFlight = false;
            leaveWLInFlight = false;
            cancelResInFlight = false;
            resolveQrInFlight = false;
        });
    }

    @Override
    public void handleServerMessage(Object message) {
        // Can be called from non-JavaFX thread
        Platform.runLater(() -> {
            try {
                Envelope env = unwrapEnvelope(message);
                if (env == null) return;

                switch (env.getOp()) {

                    // =========================
                    // ✅ VALIDATE CODE RESPONSE
                    // =========================
                case RESPONSE_TERMINAL_VALIDATE_CODE -> {
                	validateInFlight = false;
                	
                    Object payload = env.getPayload();

                    if (payload instanceof TerminalValidateResponseDTO dto) {

                    	int rid = 0;
                    	try { rid = dto.getReservationId(); } catch (Throwable ignored) {}
                    	lastValidatedIsReservation = rid > 0;

                        applyTerminalInfoToUI(dto); // keep this if you already have it

                        if (dto.isValid()) {
                            boolean canCheckIn = dto.isCheckInAllowed();
                            if (canCheckIn) {
                                setValidationState(true, true, "VALID ✅", "Code valid. Ready to check-in.");
                                lblWaitMessage.setText("");
                            } else {
                                setValidationState(true, false, "VALID ✅",
                                        nonEmptyOr(dto.getMessage(), "Valid code, but check-in not allowed (time/status rule)."));
                                lblWaitMessage.setText("");
                            }

                        } else {
                            setValidationState(false, false, "INVALID ❌",
                                    nonEmptyOr(dto.getMessage(), "Invalid code."));
                        }
                    }
                    refreshSubscriberActiveListIfNeeded();
                }

                    // =========================
                    // ✅ CHECK-IN RESPONSE
                    // =========================
                case RESPONSE_TERMINAL_CHECK_IN -> {
                    // Always unlock on response
                    checkInInFlight = false;

                    Object payload = env.getPayload();

                    // =========================================================
                    // NEW: server sends TerminalValidateResponseDTO (recommended)
                    // =========================================================
                    if (payload instanceof TerminalValidateResponseDTO dto) {

                        // After check-in, tableId should be filled by server on success
                        String tableId = dto.getTableId();
                        boolean hasTable = tableId != null && !tableId.isBlank() && !"-".equals(tableId);

                        lblTableNumber.setText(hasTable ? tableId : "-");

                        String msg = dto.getMessage();
                        if (msg == null || msg.isBlank()) {
                            msg = dto.isValid()
                                    ? (hasTable ? "Checked-in successfully." : "Check-in processed, but no table assigned.")
                                    : "Check-in failed.";
                        }
                        lblTerminalStatus.setText(msg);

                        // ✅ Mark checked-in only on TRUE success (valid + table assigned)
                        checkedIn = dto.isValid() && hasTable;
                        if (checkedIn) {
                            lastCheckedInCode = safeTrim(txtConfirmationCode.getText());
                            lblValidationResult.setText("CHECKED-IN ✅");
                        }
                        if (checkedIn) {
                            refreshSubscriberActiveListIfNeeded();
                        }

                        // Reset ability to reuse validated state (same as your original)
                        validated = false;
                        tableAvailable = false;

                        break;
                    }

                    // =========================================
                    // Fallback: old server behavior (String)
                    // =========================================
                    String msg = (payload == null) ? "" : payload.toString();

                    boolean success = containsIgnoreCase(msg, "ARRIVED") || containsIgnoreCase(msg, "SUCCESS");

                    if (success) {

                        // Optional parse "ARRIVED|T12"
                        String parsedTableId = null;

                        if (msg.contains("|")) {
                            String[] parts = msg.split("\\|");
                            if (parts.length >= 2) {
                                parsedTableId = parts[1].trim();
                            }
                        }

                        boolean hasTable = parsedTableId != null && !parsedTableId.isBlank();

                        if (hasTable) {
                            lblTableNumber.setText(parsedTableId);
                        } else {
                            // If no table in message, keep whatever is already shown or "-"
                            if (lblTableNumber.getText() == null || lblTableNumber.getText().isBlank()) {
                                lblTableNumber.setText("-");
                            }
                        }

                        lblTerminalStatus.setText(msg.isBlank() ? "Checked-in successfully." : msg);

                        // ✅ In old mode: consider success only if we have a table
                        checkedIn = hasTable;
                        if (checkedIn) {
                            lastCheckedInCode = safeTrim(txtConfirmationCode.getText());
                            lblValidationResult.setText("CHECKED-IN ✅");
                        }
                        if (checkedIn) {
                            refreshSubscriberActiveListIfNeeded();
                        }

                    } else {
                        lblTerminalStatus.setText(msg.isBlank() ? "Check-in failed." : msg);
                        checkedIn = false;
                    }

                    validated = false;
                    tableAvailable = false;
                }


                    case RESPONSE_RECOVER_CONFIRMATION_CODE -> {
                        Object payload = env.getPayload();
                        String msg = (payload == null) ? "" : payload.toString();
                        lblRecoverResult.setText(msg.isBlank() ? "Recovery response received." : msg);
                        lblTerminalStatus.setText("Recovery done.");
                        refreshSubscriberActiveListIfNeeded();
                    }

                    case RESPONSE_WAITING_LIST -> {
                        joinWLInFlight = false; // unlock on response

                        Object payload = env.getPayload();

                        if (payload instanceof common.dto.WaitingListDTO dto) {
                            String code = dto.getConfirmationCode();
                            String st = dto.getStatus();
                            int people = dto.getPeopleCount();
                            String email = dto.getEmail();
                            String phone = dto.getPhone();

                            lblTerminalStatus.setText("✅ Joined waiting list. Code: " + (code == null ? "-" : code));

                            // ✅ Fill Waiting List Details box
                            lblWaitCode.setText(code == null || code.isBlank() ? "-" : code);
                            lblWaitStatus.setText(st == null || st.isBlank() ? "-" : st);
                            lblWaitPeople.setText(people > 0 ? String.valueOf(people) : "-");
                            lblWaitEmail.setText(email == null || email.isBlank() ? "-" : email);
                            lblWaitPhone.setText(phone == null || phone.isBlank() ? "-" : phone);

                        } else {
                            String msg = (payload == null) ? "" : payload.toString();
                            lblTerminalStatus.setText(msg.isBlank() ? "Waiting list response received." : msg);
                        }
                        refreshSubscriberActiveListIfNeeded();
                    }

                    case RESPONSE_LEAVE_WAITING_LIST -> {
                        leaveWLInFlight = false; // unlock on response

                        Object payload = env.getPayload();

                        // If server returns WaitingListDTO updated
                        if (payload instanceof common.dto.WaitingListDTO dto) {
                        	refreshSubscriberActiveListIfNeeded();
                            lblTerminalStatus.setText("✅ Left waiting list. Code: " + nonEmptyOrDash(dto.getConfirmationCode()));

                            // Update waiting box
                            lblWaitStatus.setText(nonEmptyOrDash(dto.getStatus()));

                            // Optional: clear all waiting details
                            resetWaitingDetails();

                        } else {
                            // Or server returns String
                            String msg = (payload == null) ? "" : payload.toString();
                            lblTerminalStatus.setText(msg.isBlank() ? "Left waiting list." : msg);

                            resetWaitingDetails();
                        }                     
                    }
                    
                    case RESPONSE_TERMINAL_CANCEL_RESERVATION -> {
                        cancelResInFlight = false;

                        Object payload = env.getPayload();
                        String msg = (payload == null) ? "" : payload.toString();

                        lblTerminalStatus.setText(msg.isBlank() ? "Cancel response received." : msg);

                        // ✅ Reset UI after cancel (recommended)
                        validated = false;
                        tableAvailable = false;
                        checkedIn = false;
                        lastCheckedInCode = "";
                        lastValidatedCode = "";

                        lblValidationResult.setText("");
                        resetAll();
                        resetWaitingDetails();
                        lblRecoverResult.setText("");
                        
                        txtConfirmationCode.clear();
                        refreshSubscriberActiveListIfNeeded();
                    }
                    
                    case RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR -> {
                        resolveQrInFlight = false;

                        if (lstSubscriberActive == null) return;

                        Object payload = env.getPayload();

                        // Expect: Object[] { username, List<TerminalActiveItemDTO> }
                        if (payload instanceof Object[] arr && arr.length >= 2) {

                            String username = safeStr(arr[0]);
                            
                            terminalIsSubscriber = true;
                            terminalSubscriberUsername = username;

                            lblTerminalStatus.setText("Subscriber detected: " + username);

                            lstSubscriberActive.getItems().clear();

                            if (arr[1] instanceof java.util.List<?> list) {
                                for (Object o : list) {
                                    if (o instanceof TerminalActiveItemDTO dto) {
                                        lstSubscriberActive.getItems().add(dto);
                                    }
                                }
                            }

                            if (lstSubscriberActive.getItems().isEmpty()) {
                                lblTerminalStatus.setText("No active reservation or waiting list for " + username);
                            } else {
                                lblTerminalStatus.setText("Select an item to validate.");
                            }

                            return;
                        }

                        lblTerminalStatus.setText("Invalid QR resolve response.");
                        terminalIsSubscriber = false;
                        terminalSubscriberUsername = "";
                        refreshSubscriberActiveListIfNeeded();
                    }
                    
                    case RESPONSE_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES -> {

                        if (lstSubscriberActive == null) return;

                        lstSubscriberActive.getItems().clear();

                        Object payload = env.getPayload();
                        if (!(payload instanceof java.util.List<?> list)) {
                            lblTerminalStatus.setText("No active items.");
                            return;
                        }

                        for (Object o : list) {
                            if (o instanceof common.dto.TerminalActiveItemDTO dto) {
                                lstSubscriberActive.getItems().add(dto);
                            }
                        }
                    }


                    default -> {
                        // ignore
                    }
                }

                refreshConnectionLabel();

            } catch (Exception ex) {
                lblTerminalStatus.setText("UI error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    // =========================
    // Helpers
    // =========================

    private void resetWaitingDetails() {
        if (lblWaitCode != null) lblWaitCode.setText("-");
        if (lblWaitStatus != null) lblWaitStatus.setText("-");
        if (lblWaitPeople != null) lblWaitPeople.setText("-");
        if (lblWaitEmail != null) lblWaitEmail.setText("-");
        if (lblWaitPhone != null) lblWaitPhone.setText("-");
    }

    private void sendToServer(OpCode op, Object payload) {
        try {
            var client = ClientSession.getClient();
            if (client == null) {
                lblTerminalStatus.setText("ClientSession client is null.");
                validateInFlight = false;
                checkInInFlight = false;
                joinWLInFlight = false;
                leaveWLInFlight = false;
                cancelResInFlight = false;   // ✅ ADD THIS
                return;
            }

            Envelope env = Envelope.request(op, payload);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

        } catch (Exception e) {
            lblTerminalStatus.setText("Send failed: " + e.getMessage());
            e.printStackTrace();

            validateInFlight = false;
            checkInInFlight = false;
            joinWLInFlight = false;
            leaveWLInFlight = false;
            cancelResInFlight = false;   // ✅ ADD THIS
        }
    }

    /**
     * Robust unwrap:
     * - supports Envelope directly
     * - supports KryoMessage with ANY byte[] getter/field (getBytes/getPayload/getData/bytes/etc.)
     */
    private Envelope unwrapEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope env) return env;

            if (msg instanceof KryoMessage km) {
                if (!"ENVELOPE".equals(km.getType())) return null;

                byte[] bytes = extractBytesFromKryoMessage(km);
                if (bytes == null || bytes.length == 0) return null;

                // KryoUtil.fromBytes takes ONE arg
                return KryoUtil.fromBytes(bytes);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] extractBytesFromKryoMessage(KryoMessage km) {
        try {
            // 1) try common method names
            String[] methodNames = {"getBytes", "getData", "getPayload", "getBody", "bytes", "data", "payload"};
            for (String name : methodNames) {
                try {
                    Method m = km.getClass().getMethod(name);
                    Object val = m.invoke(km);
                    if (val instanceof byte[] b) return b;
                } catch (NoSuchMethodException ignored) {}
            }

            // 2) try common field names
            String[] fieldNames = {"bytes", "data", "payload", "body"};
            for (String fName : fieldNames) {
                try {
                    Field f = km.getClass().getDeclaredField(fName);
                    f.setAccessible(true);
                    Object val = f.get(km);
                    if (val instanceof byte[] b) return b;
                } catch (NoSuchFieldException ignored) {}
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isConnected() {
        try {
            var c = ClientSession.getClient();
            return c != null && c.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void refreshConnectionLabel() {
        if (lblConnectionStatus != null) {
            lblConnectionStatus.setText(isConnected() ? "Status: Connected" : "Status: Not connected");
        }
    }

    private void setValidationState(boolean isValid, boolean hasTable, String resultText, String statusText) {
        validated = isValid;
        tableAvailable = hasTable;
        lblValidationResult.setText(resultText);
        lblTerminalStatus.setText(statusText);

        if (!isValid) {
            resetDetailsOnly();
            lblTableNumber.setText("-");
            lblWaitMessage.setText("");
            return;
        }

        if (!hasTable) {
            lblTableNumber.setText("-");
            lblWaitMessage.setText("Please wait…");
        } else {
            lblWaitMessage.setText("");
        }

    }

    private void resetAll() {
        validated = false;
        tableAvailable = false;

        resetDetailsOnly();

        lblTableNumber.setText("-");
        lblWaitMessage.setText("");
        // Buttons stay enabled always
    }

    private void resetDetailsOnly() {
        if (lblResId != null) lblResId.setText("-");
        if (lblResDateTime != null) lblResDateTime.setText("-");
        if (lblResGuests != null) lblResGuests.setText("-");
    }

    // =========================
    // Reservation Details UI fill (DTO)
    // =========================
    private void applyTerminalInfoToUI(TerminalValidateResponseDTO dto) {

        int id = 0;
        try { id = dto.getReservationId(); } catch (Throwable ignored) {}
        lblResId.setText(id > 0 ? String.valueOf(id) : "-");

        lblResDateTime.setText(nonEmptyOrDash(formatTimestamp(dto.getReservationTime())));
        lblResGuests.setText(dto.getNumOfCustomers() > 0 ? String.valueOf(dto.getNumOfCustomers()) : "-");

        // Validate normally returns null tableId (Option A), but keep if server sends it
        String tableId = safeStr(dto.getTableId());
        lblTableNumber.setText(tableId.isBlank() ? "-" : tableId);

        lblWaitMessage.setText("");
    }

    // =========================
    // Small helpers
    // =========================
    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static boolean containsIgnoreCase(String text, String part) {
        if (text == null || part == null) return false;
        return text.toLowerCase().contains(part.toLowerCase());
    }

    private String formatTimestamp(Timestamp ts) {
        try {
            if (ts == null) return "";
            return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return safeStr(ts);
        }
    }

    private String nonEmptyOrDash(String s) {
        s = safeStr(s);
        return s.isBlank() ? "-" : s;
    }

    private String nonEmptyOr(String s, String fallback) {
        s = safeStr(s);
        return s.isBlank() ? fallback : s;
    }

    private static String safeStr(Object o) {
        return (o == null) ? "" : String.valueOf(o);
    }

    private static class JoinWaitingInput {
        final int people;
        final String email;
        final String phone; // optional

        JoinWaitingInput(int people, String email, String phone) {
            this.people = people;
            this.email = email;
            this.phone = phone;
        }
    }

    private JoinWaitingInput askJoinWaitingListData() {
        Dialog<JoinWaitingInput> dialog = new Dialog<>();
        dialog.setTitle("Join Waiting List");
        dialog.setHeaderText("Enter details to join the waiting list (Email OR Phone is required)");

        ButtonType okBtn = new ButtonType("Join", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField peopleField = new TextField();
        peopleField.setPromptText("e.g. 3");

        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com (optional)");

        TextField phoneField = new TextField();
        phoneField.setPromptText("05XXXXXXXX (optional)");

        grid.add(new Label("People count*:"), 0, 0);
        grid.add(peopleField, 1, 0);

        grid.add(new Label("Email (optional):"), 0, 1);
        grid.add(emailField, 1, 1);

        grid.add(new Label("Phone (optional):"), 0, 2);
        grid.add(phoneField, 1, 2);

        Label hint = new Label("At least one contact method is required: Email OR Phone.");
        hint.setStyle("-fx-font-size: 11px; -fx-opacity: 0.85;");
        grid.add(hint, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Node joinButton = dialog.getDialogPane().lookupButton(okBtn);
        joinButton.setDisable(true);

        Runnable validate = () -> {
            String p  = peopleField.getText() == null ? "" : peopleField.getText().trim();
            String e  = emailField.getText() == null ? "" : emailField.getText().trim();
            String ph = phoneField.getText() == null ? "" : phoneField.getText().trim();

            boolean okPeople = false;
            try {
                okPeople = Integer.parseInt(p) > 0;
            } catch (Exception ignored) {}

            boolean okContact = !e.isBlank() || !ph.isBlank(); // ✅ email OR phone

            joinButton.setDisable(!(okPeople && okContact));
        };

        peopleField.textProperty().addListener((o, a, b) -> validate.run());
        emailField.textProperty().addListener((o, a, b) -> validate.run());
        phoneField.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn == okBtn) {
                int people = Integer.parseInt(peopleField.getText().trim());
                String email = emailField.getText() == null ? "" : emailField.getText().trim();
                String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
                return new JoinWaitingInput(people, email, phone);
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private Integer askPeopleCountOnly() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Join Waiting List");
        d.setHeaderText("Enter number of guests");

        d.setContentText("People count:");

        return d.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
                })
                .filter(n -> n > 0)
                .orElse(null);
    }
    
    private void applyRoleVisibility() {
        // Button visible to ALL
        btnScanSubscriberQR.setVisible(true);
        btnScanSubscriberQR.setManaged(true);

        // The list can stay visible if you want, but usually keep it visible too
        subscriberActiveBox.setVisible(true);
        subscriberActiveBox.setManaged(true);
    }

    private void refreshSubscriberActiveListIfNeeded() {
        if (!terminalIsSubscriber) return;
        if (!isConnected()) return;

        sendToServer(
            OpCode.REQUEST_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES,
            terminalSubscriberUsername
        );
    }

}