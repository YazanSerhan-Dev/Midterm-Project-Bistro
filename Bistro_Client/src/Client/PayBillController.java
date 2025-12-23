package Client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class PayBillController {

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
    private boolean billLoaded = false;

    // Later: comes from login/session
    private boolean isSubscriber = true; // TODO set by login

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
    }

    private void updateCardFieldsVisibility() {
        boolean show = rbCreditCard.isSelected();
        cardFieldsPane.setDisable(!show);
        cardFieldsPane.setOpacity(show ? 1.0 : 0.5);
    }

    @FXML
    private void onFindBill() {
        String code = txtBillCode.getText().trim();
        lblPaymentResult.setText("");

        if (code.isEmpty()) {
            lblBillLookupStatus.setText("Enter a code.");
            billLoaded = false;
            btnPayNow.setDisable(true);
            clearSummary();
            return;
        }

        // UI-only demo: code "A1B2C3" exists
        if (code.equalsIgnoreCase("A1B2C3")) {
            billLoaded = true;
            lblBillLookupStatus.setText("Bill found ✅");

            double subtotal = 120.0;
            double discount = isSubscriber ? subtotal * 0.10 : 0.0;
            double total = subtotal - discount;

            lblSubscriberBadge.setText(isSubscriber ? "Subscriber: 10% OFF" : "");
            lblBillCustomerName.setText("Yazan Customer");
            lblBillItemsCount.setText("1");
            lblBillSubtotal.setText("₪" + String.format("%.2f", subtotal));
            lblBillDiscount.setText("₪" + String.format("%.2f", discount));
            lblBillTotal.setText("₪" + String.format("%.2f", total));
            lblBillDueDate.setText("2025-12-30");

            btnPayNow.setDisable(false);
        } else {
            billLoaded = false;
            lblBillLookupStatus.setText("No bill found ❌");
            btnPayNow.setDisable(true);
            clearSummary();
        }
    }

    @FXML
    private void onPayNow() {
        if (!billLoaded) return;

        // Minimal UI validation for card payment
        if (rbCreditCard.isSelected()) {
            if (txtCardNumber.getText().trim().isEmpty() ||
                txtCardExpiry.getText().trim().isEmpty() ||
                txtCardCvv.getText().trim().isEmpty() ||
                txtCardHolder.getText().trim().isEmpty()) {
                lblPaymentResult.setText("Fill all card fields.");
                return;
            }
        }

        // UI demo success
        lblPaymentResult.setText("Payment successful ✅ — table released");
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Payment");
        a.setHeaderText(null);
        a.setContentText("Payment completed successfully.\nThe table is now released (demo).");
        a.showAndWait();

        // Optional: after payment you can disable Pay Now again
        btnPayNow.setDisable(true);
    }

    @FXML
    private void onClear() {
        txtBillCode.clear();
        lblBillLookupStatus.setText("");
        lblPaymentResult.setText("");
        billLoaded = false;

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

    // Navigation
    @FXML private void onGoToTerminal() { SceneManager.showTerminal(); }
    @FXML private void onBack()         { SceneManager.showCustomerMain(); }
    @FXML private void onLogout()       { SceneManager.showLogin(); }
}

