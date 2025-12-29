package Client;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class TerminalController {

    @FXML private Label lblConnectionStatus;

    @FXML private TextField txtConfirmationCode;
    @FXML private Label lblValidationResult;

    @FXML private Label lblResName;
    @FXML private Label lblResPhone;
    @FXML private Label lblResDateTime;
    @FXML private Label lblResGuests;
    @FXML private Label lblResStatus;

    @FXML private Label lblTableNumber;
    @FXML private Label lblWaitMessage;

    @FXML private Button btnCheckIn;
    @FXML private Label lblTerminalStatus;

    // Lost code
    @FXML private TextField txtRecoverPhoneOrEmail;
    @FXML private Label lblRecoverResult;

    private boolean validated = false;
    private boolean tableAvailable = false;

    @FXML
    private void initialize() {
        lblConnectionStatus.setText("Status: Not connected (UI demo)");
        resetAll();
        lblTerminalStatus.setText("Ready.");
    }

    @FXML
    private void onValidateCode() {
        String code = txtConfirmationCode.getText().trim();
        if (code.isEmpty()) {
            lblValidationResult.setText("Enter a code.");
            lblTerminalStatus.setText("Validation failed: empty code.");
            return;
        }

        // UI-only demo behavior:
        // - A1B2C3 -> valid and table available
        // - WAIT123 -> valid but no table available (waiting)
        // - anything else -> not found
        if (code.equalsIgnoreCase("A1B2C3")) {
            validated = true;
            tableAvailable = true;

            lblValidationResult.setText("VALID ✅");
            lblTerminalStatus.setText("Code validated. Table is available.");

            // Demo reservation details
            lblResName.setText("Yazan Customer");
            lblResPhone.setText("05X-XXXXXXX");
            lblResDateTime.setText("2025-12-28 21:00");
            lblResGuests.setText("2");
            lblResStatus.setText("Confirmed");

            // Demo table assignment
            lblTableNumber.setText("12");
            lblWaitMessage.setText("");

            btnCheckIn.setDisable(false);

        } else if (code.equalsIgnoreCase("WAIT123")) {
            validated = true;
            tableAvailable = false;

            lblValidationResult.setText("VALID ✅");
            lblTerminalStatus.setText("Code validated. No suitable table is free now.");

            // Demo reservation details
            lblResName.setText("Waiting Customer");
            lblResPhone.setText("05X-1111111");
            lblResDateTime.setText("2025-12-28 21:30");
            lblResGuests.setText("4");
            lblResStatus.setText("Confirmed");

            // Waiting state
            lblTableNumber.setText("-");
            lblWaitMessage.setText("Please wait…");

            // Check-in disabled until table becomes available
            btnCheckIn.setDisable(true);

        } else {
            validated = false;
            tableAvailable = false;

            lblValidationResult.setText("NOT FOUND ❌");
            lblTerminalStatus.setText("Code not found.");
            resetDetailsOnly();
            btnCheckIn.setDisable(true);
        }
    }

    @FXML
    private void onCheckIn() {
        if (!validated || !tableAvailable) return;

        lblResStatus.setText("Checked-in");
        lblTerminalStatus.setText("Checked-in successfully (demo). Table " + lblTableNumber.getText() + " assigned.");
    }

    @FXML
    private void onRecoverCode() {
        String key = txtRecoverPhoneOrEmail.getText().trim();
        if (key.isEmpty()) {
            lblRecoverResult.setText("Enter phone/email first.");
            return;
        }

        // Story says send via Email + SMS (demo simulation)
        lblRecoverResult.setText("Code sent to SMS + Email (demo). Latest code: A1B2C3");
        lblTerminalStatus.setText("Recovery requested (demo).");
    }

    @FXML
    private void onClear() {
        txtConfirmationCode.clear();
        lblValidationResult.setText("");
        resetAll();
        lblTerminalStatus.setText("Cleared.");
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
        lblResName.setText("-");
        lblResPhone.setText("-");
        lblResDateTime.setText("-");
        lblResGuests.setText("-");
        lblResStatus.setText("-");
    }

    // Navigation
    @FXML private void onGoToPayBill() { SceneManager.showPayBill(); }
    @FXML private void onBack()        { SceneManager.showCustomerMain(); }
    @FXML private void onLogout()      { SceneManager.showLogin(); }
}

