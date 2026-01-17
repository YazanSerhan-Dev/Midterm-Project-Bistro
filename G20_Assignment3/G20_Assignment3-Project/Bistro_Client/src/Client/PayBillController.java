package Client;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.BillDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
/**
 * JavaFX controller for the "Pay Bill" screen.
 * <p>
 * Allows a user to search for a bill by code, display bill summary details,
 * and submit a payment request (cash or credit card). Communicates with the server
 * using the shared {@link BistroClient} connection managed by {@link ClientSession}.
 * </p>
 * <p>
 * Implements {@link ClientUI} to receive server responses related to bill lookup and payment.
 * </p>
 */
public class PayBillController implements ClientUI {

    @FXML private TextField txtBillCode;
    @FXML private Label lblBillLookupStatus;

    @FXML private Label lblSubscriberBadge;
    @FXML private Label lblBillCustomerName;
    @FXML private Label lblBillItemsCount;
    @FXML private Label lblBillSubtotal;
    @FXML private Label lblBillDiscount;
    @FXML private Label lblBillTotal;
    @FXML private Label lblBillDueDate;

    @FXML private RadioButton rbCreditCard;
    @FXML private RadioButton rbCash;
    @FXML private GridPane cardFieldsPane;

    @FXML private TextField txtCardNumber;
    @FXML private TextField txtCardExpiry;
    @FXML private TextField txtCardCvv;
    @FXML private TextField txtCardHolder;

    @FXML private Button btnPayNow;
    @FXML private Label lblPaymentResult;

    private ToggleGroup paymentGroup;

    private volatile boolean lookupInFlight = false;
    private volatile boolean payInFlight = false;

    private volatile boolean billLoaded = false;
    private volatile boolean alreadyPaid = false;

    /**
     * Initializes the controller after FXML loading.
     * Configures the payment toggle group, sets initial UI state,
     * and binds this controller as the active UI receiver for the shared session.
     */
    @FXML
    private void initialize() {
        paymentGroup = new ToggleGroup();
        rbCreditCard.setToggleGroup(paymentGroup);
        rbCash.setToggleGroup(paymentGroup);

        rbCreditCard.setSelected(true);
        updateCardFieldsVisibility();

        paymentGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> updateCardFieldsVisibility());

        clearSummary();
        btnPayNow.setDisable(true);
        lblBillLookupStatus.setText("");
        lblPaymentResult.setText("");

        // register UI receiver
        ClientSession.bindUI(this);
    }
    /**
     * Updates the visibility and enabled state of credit card input fields
     * based on the selected payment method.
     */
    private void updateCardFieldsVisibility() {
        boolean show = rbCreditCard.isSelected();
        cardFieldsPane.setDisable(!show);
        cardFieldsPane.setOpacity(show ? 1.0 : 0.5);
    }

    // =========================
    // Actions
    // =========================
    /**
     * UI action: searches for a bill by the code entered by the user.
     * Sends a bill-lookup request to the server and resets UI summary fields while loading.
     */
    @FXML
    private void onFindBill() {
        if (lookupInFlight) return;

        String code = safeTrim(txtBillCode.getText());
        lblPaymentResult.setText("");

        if (code.isBlank()) {
            lblBillLookupStatus.setText("Enter a code.");
            billLoaded = false;
            alreadyPaid = false;
            btnPayNow.setDisable(true);
            clearSummary();
            return;
        }

        if (!isConnected()) {
            lblBillLookupStatus.setText("Not connected.");
            return;
        }

        lookupInFlight = true;
        lblBillLookupStatus.setText("Searching...");
        clearSummary();
        btnPayNow.setDisable(true);
        billLoaded = false;
        alreadyPaid = false;

        sendToServer(OpCode.REQUEST_BILL_GET_BY_CODE, code);
    }
    /**
     * UI action: submits a payment request for the currently loaded bill.
     * Performs minimal validation for credit card fields on the client side,
     * then sends a payment request to the server.
     */
    @FXML
    private void onPayNow() {
        if (payInFlight) return;
        if (!billLoaded || alreadyPaid) return;

        // Minimal UI validation for card payment
        if (rbCreditCard.isSelected()) {
        	if (rbCreditCard.isSelected()) {

        	    String card = safeTrim(txtCardNumber.getText());
        	    String expiry = safeTrim(txtCardExpiry.getText());
        	    String cvv = safeTrim(txtCardCvv.getText());
        	    String holder = safeTrim(txtCardHolder.getText());

        	    if (card.isBlank() || expiry.isBlank() || cvv.isBlank() || holder.isBlank()) {
        	        lblPaymentResult.setText("Fill all card fields.");
        	        return;
        	    }

        	    if (!isValidCardNumber(card)) {
        	        lblPaymentResult.setText("Invalid card number.");
        	        return;
        	    }

        	    if (!isValidExpiry(expiry)) {
        	        lblPaymentResult.setText("Invalid or expired card date (MM/YY).");
        	        return;
        	    }

        	    if (!isValidCvv(cvv)) {
        	        lblPaymentResult.setText("Invalid CVV.");
        	        return;
        	    }
        	}
        }

        if (!isConnected()) {
            lblPaymentResult.setText("Not connected.");
            return;
        }

        payInFlight = true;
        lblPaymentResult.setText("Paying...");

        String code = safeTrim(txtBillCode.getText());
        String method = rbCash.isSelected() ? "CASH" : "CARD";

        // We send only what server needs now (code + method)
        Object[] payload = new Object[] { code, method };

        sendToServer(OpCode.REQUEST_PAY_BILL, payload);
    }
    /**
     * UI action: clears the bill code, resets status flags, clears the summary,
     * and restores the default payment method selection.
     */
    @FXML
    private void onClear() {
        txtBillCode.clear();
        lblBillLookupStatus.setText("");
        lblPaymentResult.setText("");

        lookupInFlight = false;
        payInFlight = false;
        billLoaded = false;
        alreadyPaid = false;

        clearSummary();
        btnPayNow.setDisable(true);

        txtCardNumber.clear();
        txtCardExpiry.clear();
        txtCardCvv.clear();
        txtCardHolder.clear();

        rbCreditCard.setSelected(true);
        updateCardFieldsVisibility();
    }
    /**
     * Clears the bill summary labels and resets them to default placeholder values.
     */
    private void clearSummary() {
        lblSubscriberBadge.setText("");
        lblBillCustomerName.setText("-");
        lblBillItemsCount.setText("-");
        lblBillSubtotal.setText("-");
        lblBillDiscount.setText("-");
        lblBillTotal.setText("-");
        lblBillDueDate.setText("-");
    }
    /**
     * Applies the given bill data to the UI summary section.
     *
     * @param b bill DTO returned from the server (may be null)
     */
    private void applyBillToUI(BillDTO b) {
        if (b == null) {
            clearSummary();
            return;
        }

        lblSubscriberBadge.setText(b.isSubscriberDiscountApplied() ? "Subscriber: 10% OFF" : "");
        lblBillCustomerName.setText(nz(b.getCustomerName(), "-"));
        lblBillItemsCount.setText(String.valueOf(b.getItemsCount()));
        lblBillSubtotal.setText("₪" + String.format("%.2f", b.getSubtotal()));
        lblBillDiscount.setText("₪" + String.format("%.2f", b.getDiscount()));
        lblBillTotal.setText("₪" + String.format("%.2f", b.getTotal()));
        lblBillDueDate.setText(nz(b.getDueDate(), "-"));
    }

    // =========================
    // ClientUI
    // =========================

    /**
     * Connection callback: not used for this screen.
     */
    @Override public void onConnected() {}
    /**
     * Connection callback: resets in-flight flags when disconnected.
     */
    @Override public void onDisconnected() {
        Platform.runLater(() -> {
            lookupInFlight = false;
            payInFlight = false;
        });
    }

    /**
     * Connection callback: resets in-flight flags when a connection error occurs.
     *
     * @param e the exception describing the connection error
     */
    @Override public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            lookupInFlight = false;
            payInFlight = false;
        });
    }
    /**
     * Handles server responses related to bill lookup and bill payment
     * and updates the UI accordingly.
     *
     * @param message raw message object received from the server
     */
    @Override
    public void handleServerMessage(Object message) {
        Platform.runLater(() -> {
            Envelope env = unwrapEnvelope(message);
            if (env == null) return;

            switch (env.getOp()) {

                case RESPONSE_BILL_GET_BY_CODE -> {
                    lookupInFlight = false;

                    Object payload = env.getPayload();
                    // server sends Object[] { ok, alreadyPaid, message, BillDTO }
                    if (payload instanceof Object[] arr && arr.length >= 4) {
                        boolean ok = (arr[0] instanceof Boolean b) && b;
                        boolean paid = (arr[1] instanceof Boolean b) && b;
                        String msg = (arr[2] == null) ? "" : arr[2].toString();
                        BillDTO bill = (arr[3] instanceof BillDTO dto) ? dto : null;

                        lblBillLookupStatus.setText(msg.isBlank() ? (ok ? "Bill found ✅" : "Not found ❌") : msg);

                        if (ok && bill != null) {
                            billLoaded = true;
                            alreadyPaid = paid;
                            applyBillToUI(bill);

                            if (paid) {
                                btnPayNow.setDisable(true);
                                lblPaymentResult.setText("Already paid ✅");
                            } else {
                                btnPayNow.setDisable(false);
                                lblPaymentResult.setText("");
                            }
                        } else {
                            billLoaded = false;
                            alreadyPaid = false;
                            clearSummary();
                            btnPayNow.setDisable(true);
                        }
                    } else {
                        lblBillLookupStatus.setText("Bad response from server.");
                        billLoaded = false;
                        alreadyPaid = false;
                        btnPayNow.setDisable(true);
                        clearSummary();
                    }
                }

                case RESPONSE_PAY_BILL -> {
                    payInFlight = false;

                    Object payload = env.getPayload();
                    // server sends Object[] { ok, message, tableId }
                    if (payload instanceof Object[] arr && arr.length >= 2) {
                        boolean ok = (arr[0] instanceof Boolean b) && b;
                        String msg = (arr[1] == null) ? "" : arr[1].toString();

                        lblPaymentResult.setText(msg.isBlank() ? (ok ? "Payment successful ✅" : "Payment failed ❌") : msg);

                        if (ok) {
                            alreadyPaid = true;
                            btnPayNow.setDisable(true);

                            Alert a = new Alert(Alert.AlertType.INFORMATION);
                            a.setTitle("Payment");
                            a.setHeaderText(null);
                            a.setContentText(msg.isBlank() ? "Payment completed successfully." : msg);
                            a.showAndWait();
                        }
                    } else {
                        lblPaymentResult.setText("Bad response from server.");
                    }
                }

                default -> {}
            }
        });
    }

    // =========================
    // Envelope helpers (simple)
    // =========================
    /**
     * Sends a request envelope to the server using the shared client connection.
     *
     * @param op operation code indicating the request type
     * @param payload request payload (may be a string or an object array depending on request)
     */
    private void sendToServer(OpCode op, Object payload) {
        try {
            var client = ClientSession.getClient();
            Envelope env = Envelope.request(op, payload);
            client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));
        } catch (Exception e) {
            lookupInFlight = false;
            payInFlight = false;
            lblPaymentResult.setText("Send failed: " + e.getMessage());
        }
    }
    /**
     * Attempts to decode a raw server message into an {@link Envelope}.
     *
     * @param msg raw message received from the server
     * @return decoded envelope or null if the message is not supported/decoding fails
     */
    private Envelope unwrapEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope e) return e;
            if (msg instanceof KryoMessage km) {
                if (!"ENVELOPE".equals(km.getType())) return null;
                return KryoUtil.fromBytes(km.getPayload());
            }
        } catch (Exception ignored) {}
        return null;
    }
    /**
     * Checks whether the shared client connection is currently active.
     *
     * @return true if the client exists and is connected, otherwise false
     */
    private boolean isConnected() {
        try {
            var c = ClientSession.getClient();
            return c != null && c.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Trims a string safely (null becomes empty string).
     *
     * @param s input string
     * @return trimmed string or empty string if null
     */
    private static String safeTrim(String s) { return s == null ? "" : s.trim(); }
    /**
     * Returns a fallback value when a string is null or blank.
     *
     * @param s input string
     * @param fallback fallback value
     * @return s if non-blank, otherwise fallback
     */
    private static String nz(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }
    
 // =========================
 // Payment validation (Level 1 - looks valid)
 // =========================
    /**
     * Normalizes a card number input by removing spaces and dashes.
     *
     * @param input raw card number input
     * @return normalized digits-only string (may be empty)
     */
	 private static String normalizeCardNumber(String input) {
	     if (input == null) return "";
	     return input.replaceAll("[\\s-]", "");
	 }
	    /**
	     * Validates card number format and performs a Luhn check.
	     *
	     * @param input raw card number input
	     * @return true if the number length is valid and passes the Luhn algorithm
	     */
	 private static boolean isValidCardNumber(String input) {
	     String n = normalizeCardNumber(input);
	     if (!n.matches("^\\d{13,19}$")) return false;
	     return passesLuhn(n);
	 }
	    /**
	     * Performs the Luhn algorithm checksum validation.
	     *
	     * @param digits card number digits (no spaces/dashes)
	     * @return true if checksum is valid, otherwise false
	     */
	 private static boolean passesLuhn(String digits) {
	     int sum = 0;
	     boolean doubleDigit = false;
	
	     for (int i = digits.length() - 1; i >= 0; i--) {
	         int d = digits.charAt(i) - '0';
	
	         if (doubleDigit) {
	             d *= 2;
	             if (d > 9) d -= 9;
	         }
	
	         sum += d;
	         doubleDigit = !doubleDigit;
	     }
	     return sum % 10 == 0;
	 }
	    /**
	     * Validates expiry date format (MM/YY or MM/YYYY) and checks it is not expired.
	     *
	     * @param expiry expiry string
	     * @return true if the format is valid and the date is not in the past
	     */
	 private static boolean isValidExpiry(String expiry) {
	     if (expiry == null) return false;
	     String e = expiry.trim();
	
	     if (!e.matches("^\\d{2}/(\\d{2}|\\d{4})$")) return false;
	
	     String[] parts = e.split("/");
	     int month = Integer.parseInt(parts[0]);
	     if (month < 1 || month > 12) return false;
	
	     int year = Integer.parseInt(parts[1]);
	     if (parts[1].length() == 2) year += 2000;
	
	     java.time.YearMonth exp = java.time.YearMonth.of(year, month);
	     java.time.YearMonth now = java.time.YearMonth.now();
	
	     return !exp.isBefore(now);
	 }
	    /**
	     * Validates CVV format (3 or 4 digits).
	     *
	     * @param cvv cvv input string
	     * @return true if CVV is numeric and has 3-4 digits
	     */
	 private static boolean isValidCvv(String cvv) {
	     if (cvv == null) return false;
	     return cvv.trim().matches("^\\d{3,4}$");
	 }
	

    // Navigation (keep your current behavior)

    /**
	* UI action: navigates back to the previous screen.
	*/
    @FXML private void onBack()         { SceneManager.goBack(); }
    /**
     * UI action: logs out and returns to the login screen.
     */
    @FXML private void onLogout()       { SceneManager.showLogin(); }
}
