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

    private void updateCardFieldsVisibility() {
        boolean show = rbCreditCard.isSelected();
        cardFieldsPane.setDisable(!show);
        cardFieldsPane.setOpacity(show ? 1.0 : 0.5);
    }

    // =========================
    // Actions
    // =========================

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

    private void clearSummary() {
        lblSubscriberBadge.setText("");
        lblBillCustomerName.setText("-");
        lblBillItemsCount.setText("-");
        lblBillSubtotal.setText("-");
        lblBillDiscount.setText("-");
        lblBillTotal.setText("-");
        lblBillDueDate.setText("-");
    }

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

    @Override public void onConnected() {}
    @Override public void onDisconnected() {
        Platform.runLater(() -> {
            lookupInFlight = false;
            payInFlight = false;
        });
    }
    @Override public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            lookupInFlight = false;
            payInFlight = false;
        });
    }

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

    private boolean isConnected() {
        try {
            var c = ClientSession.getClient();
            return c != null && c.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private static String safeTrim(String s) { return s == null ? "" : s.trim(); }
    private static String nz(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }
    
 // =========================
 // Payment validation (Level 1 - looks valid)
 // =========================

	 private static String normalizeCardNumber(String input) {
	     if (input == null) return "";
	     return input.replaceAll("[\\s-]", "");
	 }
	
	 private static boolean isValidCardNumber(String input) {
	     String n = normalizeCardNumber(input);
	     if (!n.matches("^\\d{13,19}$")) return false;
	     return passesLuhn(n);
	 }
	
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
	
	 private static boolean isValidCvv(String cvv) {
	     if (cvv == null) return false;
	     return cvv.trim().matches("^\\d{3,4}$");
	 }
	

    // Navigation (keep your current behavior)
    @FXML private void onBack()         { SceneManager.goBack(); }
    @FXML private void onLogout()       { SceneManager.showLogin(); }
}
