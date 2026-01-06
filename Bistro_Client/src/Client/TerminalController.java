package Client;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.ReservationDTO;
import common.dto.TerminalValidateResponseDTO;
import common.dto.WaitingListDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

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



    private volatile boolean validated = false;
    private volatile boolean tableAvailable = false;

    @FXML
    private void initialize() {
        refreshConnectionLabel();
        resetAll();
        lblTerminalStatus.setText("Ready.");

        // Register this screen as the current UI receiver
        ClientSession.bindUI(this);
    }
  
    // =========================
    // UI Actions
    // =========================
    
    @FXML
    private void onJoinWaitingList() {
        if (!isConnected()) {
            lblTerminalStatus.setText("Not connected.");
            return;
        }

        String role = ClientSession.getRole();        // SUBSCRIBER / CUSTOMER
        String username = ClientSession.getUsername(); // subscriber only

        try {
            WaitingListDTO dto = new WaitingListDTO();

            if ("SUBSCRIBER".equalsIgnoreCase(role)) {

                Integer people = askPeopleCountOnly(); // your simple dialog
                if (people == null) {
                    lblTerminalStatus.setText("Cancelled.");
                    return;
                }
                dto.setPeopleCount(people);

                // subscriber doesn't enter contact info
                dto.setEmail("");
                dto.setPhone("");

            } else {
                JoinWaitingInput in = askJoinWaitingListData(); // your dialog
                if (in == null) {
                    lblTerminalStatus.setText("Cancelled.");
                    return;
                }

                // CUSTOMER: email + phone REQUIRED
                dto.setPeopleCount(in.people);
                dto.setEmail(in.email);
                dto.setPhone(in.phone);
            }

            Object[] payload = new Object[] { role, username, dto };

            Envelope env = Envelope.request(OpCode.REQUEST_WAITING_LIST, payload);
            ClientSession.getClient().sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblTerminalStatus.setText("Joining waiting list...");

        } catch (Exception e) {
            lblTerminalStatus.setText("Failed: " + e.getMessage());
        }
    }


    @FXML
    private void onLeaveWaitingList() {
        if (!isConnected()) {
            lblTerminalStatus.setText("Not connected.");
            return;
        }

        // Use the code already displayed after join
        String code = safeTrim(lblWaitCode == null ? "" : lblWaitCode.getText());

        // If user didn't join in this session, fallback to typed confirmation code (optional)
        if (code.isEmpty() || "-".equals(code)) {
            code = safeTrim(txtConfirmationCode.getText());
        }

        if (code.isEmpty()) {
            lblTerminalStatus.setText("No waiting code found. Join waiting list first.");
            return;
        }

        // Build payload: role + username + confirmationCode
        // (matches your join pattern that sends [role, username, dto])
        String role = ClientSession.getRole();          // SUBSCRIBER / CUSTOMER
        String username = ClientSession.getUsername();  // subscriber username (customer may be null/empty)

        Object[] payload = new Object[] { role, username, code };

        Envelope env = Envelope.request(OpCode.REQUEST_LEAVE_WAITING_LIST, payload);

        try {
            ClientSession.getClient().sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblTerminalStatus.setText("Leaving waiting list...");
            btnLeaveWaitingList.setDisable(true);

        } catch (Exception e) {
            lblTerminalStatus.setText("Failed: " + e.getMessage());
            btnLeaveWaitingList.setDisable(false);
        }
    }

    @FXML
    private void onValidateCode() {
        String code = safeTrim(txtConfirmationCode.getText());
        if (code.isEmpty()) {
            setValidationState(false, false, "Enter a code.", "Validation failed: empty code.");
            return;
        }

        if (!isConnected()) {
            setValidationState(false, false, "Not connected.", "Cannot validate: not connected to server.");
            return;
        }

        sendToServer(OpCode.REQUEST_TERMINAL_VALIDATE_CODE, code);

        lblValidationResult.setText("Checking...");
        lblTerminalStatus.setText("Validating code...");
        btnCheckIn.setDisable(true);
    }

    @FXML
    private void onCheckIn() {
        if (!validated) {
            lblTerminalStatus.setText("Check-in blocked: validate code first.");
            return;
        }

        String code = safeTrim(txtConfirmationCode.getText());
        if (code.isEmpty()) {
            lblTerminalStatus.setText("Check-in blocked: empty code.");
            return;
        }

        if (!isConnected()) {
            lblTerminalStatus.setText("Cannot check-in: not connected to server.");
            return;
        }

        sendToServer(OpCode.REQUEST_TERMINAL_CHECK_IN, code);

        lblTerminalStatus.setText("Sending check-in request...");
        btnCheckIn.setDisable(true);
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
        txtConfirmationCode.clear();
        lblValidationResult.setText("");
        resetAll();
        lblTerminalStatus.setText("Cleared.");
        refreshConnectionLabel();
    }

    // Navigation
    @FXML private void onGoToPayBill() { SceneManager.showPayBill(); }
    @FXML private void onBack()        { SceneManager.showCustomerMain(); }
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
            btnCheckIn.setDisable(true);
        });
    }

    @Override
    public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            refreshConnectionLabel();
            lblTerminalStatus.setText("Connection error: " + (e == null ? "" : e.getMessage()));
            btnCheckIn.setDisable(true);
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
                    Object payload = env.getPayload();

                    // ✅ Server sends TerminalValidateResponseDTO (preferred)
                    if (payload instanceof TerminalValidateResponseDTO dto) {

                        // Fill reservation/waiting details (ID / time / guests / status)
                        applyTerminalInfoToUI(dto);

                        if (dto.isValid()) {
                            validated = true;
                            lblValidationResult.setText("VALID ✅");

                            boolean canCheckIn = dto.isCheckInAllowed();
                            if (canCheckIn) {
                                tableAvailable = true;
                                lblWaitMessage.setText("");
                                btnCheckIn.setDisable(false);
                                lblTerminalStatus.setText("Code valid. Ready to check-in.");
                            } else {
                                tableAvailable = false;
                                btnCheckIn.setDisable(true);

                                String m = safeStr(dto.getMessage());
                                lblWaitMessage.setText("");
                                lblTerminalStatus.setText(
                                        m.isBlank()
                                                ? "Valid code, but check-in not allowed (time/status rule)."
                                                : m
                                );
                            }

                        } else {
                            // ✅ INVALID DTO: could be "not found" OR "expired waiting code"
                            validated = false;
                            tableAvailable = false;

                            String st = safeStr(dto.getStatus()).trim();
                            boolean isWaitingCode = dto.getReservationId() == 0;

                            if (isWaitingCode && "CANCELED".equalsIgnoreCase(st)) {
                                lblValidationResult.setText("EXPIRED ⏱");
                                lblTerminalStatus.setText(nonEmptyOr(dto.getMessage(),
                                        "This waiting code has expired or was canceled."));
                                lblWaitMessage.setText("");
                                btnCheckIn.setDisable(true);
                                lblTableNumber.setText("-");
                                // IMPORTANT: do NOT resetDetailsOnly() -> we want waiting details to remain visible
                            } else {
                                lblValidationResult.setText("NOT FOUND ❌");
                                lblTerminalStatus.setText(nonEmptyOr(dto.getMessage(), "Code not found."));
                                resetDetailsOnly();
                                lblTableNumber.setText("-");
                                lblWaitMessage.setText("");
                                btnCheckIn.setDisable(true);
                            }
                        }

                        break;
                    }

                    // ✅ OLD: if server sends ReservationDTO
                    if (payload instanceof ReservationDTO res) {
                        applyReservationToUI(res);

                        validated = true;
                        lblValidationResult.setText("VALID ✅");

                        String st = safeStr(getReservationStatus(res));
                        boolean canCheckIn = "CONFIRMED".equalsIgnoreCase(st);

                        if (canCheckIn) {
                            tableAvailable = true;
                            lblWaitMessage.setText("");
                            btnCheckIn.setDisable(false);
                            lblTerminalStatus.setText("Code valid. Ready to check-in.");
                        } else {
                            tableAvailable = false;
                            btnCheckIn.setDisable(true);
                            lblWaitMessage.setText("");
                            lblTerminalStatus.setText("Valid code, but status is " + st + " (check-in not allowed).");
                        }

                        break;
                    }

                    // ✅ FALLBACK: server sends String (no dto access here!)
                    String msg = (payload == null) ? "" : payload.toString();

                    if (containsIgnoreCase(msg, "VALID") || containsIgnoreCase(msg, "SUCCESS")) {
                        validated = true;

                        if (containsIgnoreCase(msg, "WAIT") || containsIgnoreCase(msg, "NO TABLE")) {
                            tableAvailable = false;
                            lblValidationResult.setText("VALID ✅");
                            lblWaitMessage.setText("Please wait…");
                            lblTableNumber.setText("-");
                            btnCheckIn.setDisable(true);
                            lblTerminalStatus.setText(msg.isBlank() ? "Validated. Waiting for table." : msg);
                        } else {
                            tableAvailable = true;
                            lblValidationResult.setText("VALID ✅");
                            lblWaitMessage.setText("");
                            btnCheckIn.setDisable(false);
                            lblTerminalStatus.setText(msg.isBlank() ? "Code validated." : msg);
                        }

                    } else {
                        validated = false;
                        tableAvailable = false;
                        lblValidationResult.setText("NOT FOUND ❌");
                        lblTerminalStatus.setText(msg.isBlank() ? "Code not found." : msg);
                        resetDetailsOnly();
                        lblTableNumber.setText("-");
                        lblWaitMessage.setText("");
                        btnCheckIn.setDisable(true);
                    }
                }


                    // =========================
                    // ✅ CHECK-IN RESPONSE
                    // =========================
                    case RESPONSE_TERMINAL_CHECK_IN -> {
                        Object payload = env.getPayload();

                        // NEW: server sends DTO (recommended)
                        if (payload instanceof TerminalValidateResponseDTO dto) {

                            // After check-in, tableId should be filled by server
                            String tableId = dto.getTableId();
                            lblTableNumber.setText((tableId == null || tableId.isBlank()) ? "-" : tableId);

                            String msg = dto.getMessage();
                            if (msg == null || msg.isBlank()) msg = "Checked-in successfully (ARRIVED).";
                            lblTerminalStatus.setText(msg);

                            btnCheckIn.setDisable(true);
                            validated = false;
                            tableAvailable = false;
                            break;
                        }

                        // fallback: old server behavior (String)
                        String msg = (payload == null) ? "" : payload.toString();

                        if (containsIgnoreCase(msg, "ARRIVED") || containsIgnoreCase(msg, "SUCCESS")) {

                            // Optional: parse "ARRIVED|T12"
                            if (msg.contains("|")) {
                                String[] parts = msg.split("\\|");
                                if (parts.length >= 2) {
                                    String tableId = parts[1].trim();
                                    lblTableNumber.setText(tableId.isBlank() ? "-" : tableId);
                                }
                            }

                            lblTerminalStatus.setText(msg.isBlank() ? "Checked-in successfully (ARRIVED)." : msg);
                        } else {
                            lblTerminalStatus.setText(msg.isBlank() ? "Check-in failed." : msg);
                        }

                        btnCheckIn.setDisable(true);
                        validated = false;
                        tableAvailable = false;
                    }

                    case RESPONSE_RECOVER_CONFIRMATION_CODE -> {
                        Object payload = env.getPayload();
                        String msg = (payload == null) ? "" : payload.toString();
                        lblRecoverResult.setText(msg.isBlank() ? "Recovery response received." : msg);
                        lblTerminalStatus.setText("Recovery done.");
                    }
                    
                    case RESPONSE_WAITING_LIST -> {
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

                            // leave enabled if you want
                            if (btnLeaveWaitingList != null) btnLeaveWaitingList.setDisable(false);

                        } else {
                            String msg = (payload == null) ? "" : payload.toString();
                            lblTerminalStatus.setText(msg.isBlank() ? "Waiting list response received." : msg);
                        }
                    }
                    
                    case RESPONSE_LEAVE_WAITING_LIST -> {
                        Object payload = env.getPayload();

                        // If server returns WaitingListDTO updated
                        if (payload instanceof common.dto.WaitingListDTO dto) {
                            lblTerminalStatus.setText("✅ Left waiting list. Code: " + nonEmptyOrDash(dto.getConfirmationCode()));

                            // Update waiting box
                            lblWaitStatus.setText(nonEmptyOrDash(dto.getStatus()));

                            // Disable leave, clear fields if you want
                            btnLeaveWaitingList.setDisable(true);
                            // Optional: clear all waiting details
                            resetWaitingDetails();

                        } else {
                            // Or server returns String
                            String msg = (payload == null) ? "" : payload.toString();
                            lblTerminalStatus.setText(msg.isBlank() ? "Left waiting list." : msg);

                            btnLeaveWaitingList.setDisable(true);
                            resetWaitingDetails();
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
                return;
            }

            Envelope env = Envelope.request(op, payload);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

        } catch (Exception e) {
            lblTerminalStatus.setText("Send failed: " + e.getMessage());
            e.printStackTrace();
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
            btnCheckIn.setDisable(true);
            return;
        }

        if (!hasTable) {
            lblTableNumber.setText("-");
            lblWaitMessage.setText("Please wait…");
            btnCheckIn.setDisable(true);
        } else {
            lblWaitMessage.setText("");
            btnCheckIn.setDisable(false);
        }
    }

    private void resetAll() {
        validated = false;
        tableAvailable = false;

        resetDetailsOnly();

        lblTableNumber.setText("-");
        lblWaitMessage.setText("");

        btnCheckIn.setDisable(true);
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
    // Existing ReservationDTO UI filler (kept, but only fills the 3 fields we show)
    // =========================
    private void applyReservationToUI(ReservationDTO res) {
        // ReservationDTO may not have reservationId; keep "-"
        lblResId.setText("-");

        lblResDateTime.setText(nonEmptyOrDash(getReservationTime(res)));
        lblResGuests.setText(nonEmptyOrDash(getGuests(res)));

        String tableId = safeStr(getTableId(res));
        lblTableNumber.setText(tableId.isBlank() ? "-" : tableId);

        lblWaitMessage.setText("");
    }

    // ---- DTO getter adapters (ReservationDTO) ----
    private String getReservationTime(ReservationDTO r) {
        try { return safeStr(r.getReservationTime()); } catch (Throwable ignored) {}
        return "-";
    }

    private String getGuests(ReservationDTO r) {
        try { return String.valueOf(r.getNumOfCustomers()); } catch (Throwable ignored) {}
        return "-";
    }

    private String getReservationStatus(ReservationDTO r) {
        try { return safeStr(r.getStatus()); } catch (Throwable ignored) {}
        return "-";
    }

    private String getTableId(ReservationDTO r) { return "-"; }

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
        dialog.setHeaderText("Enter details to join the waiting list");

        ButtonType okBtn = new ButtonType("Join", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField peopleField = new TextField();
        peopleField.setPromptText("e.g. 3");

        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com (required)");

        TextField phoneField = new TextField();
        phoneField.setPromptText("05XXXXXXXX (optional)");

        grid.add(new Label("People count*:"), 0, 0);
        grid.add(peopleField, 1, 0);

        grid.add(new Label("Email*:"), 0, 1);
        grid.add(emailField, 1, 1);

        grid.add(new Label("Phone (optional):"), 0, 2);
        grid.add(phoneField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Disable Join button until required fields are valid
        Node joinButton = dialog.getDialogPane().lookupButton(okBtn);
        joinButton.setDisable(true);

        Runnable validate = () -> {
            String p = peopleField.getText() == null ? "" : peopleField.getText().trim();
            String e = emailField.getText() == null ? "" : emailField.getText().trim();
            boolean ok = false;
            try {
                ok = !e.isBlank() && Integer.parseInt(p) > 0;
            } catch (Exception ignored) {}
            joinButton.setDisable(!ok);
        };

        peopleField.textProperty().addListener((o, a, b) -> validate.run());
        emailField.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn == okBtn) {
                int people = Integer.parseInt(peopleField.getText().trim());
                String email = emailField.getText().trim();
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

}




