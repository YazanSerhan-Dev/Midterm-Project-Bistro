package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements ClientUI {

    @FXML private Label lblConn;
    @FXML private Label lblMsg;
    @FXML private TextField tfUsername;
    @FXML private PasswordField pfPassword;

    @FXML
    public void initialize() {
        // Auto connect as soon as login view loads
        try {
            lblConn.setText("Connecting...");
            ClientSession.connect(this);
        } catch (Exception e) {
            lblConn.setText("Connection failed: " + e.getMessage());
        }
    }

    @FXML
    public void onLogin(ActionEvent e) {
        String user = tfUsername.getText();
        String pass = pfPassword.getText();

        if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
            lblMsg.setText("Please enter username + password");
            return;
        }

        // TODO: send login request to server (depends on your OpCode/DTO)
        // Example idea:
        // ClientSession.getClient().sendToServer(new Envelope(OpCode.LOGIN, new LoginDTO(user, pass)));

        // For now just move to main screen to prove flow works:
        SceneManager.showCustomerMain();
    }

    // ===== ClientUI callbacks =====
    @Override
    public void onConnected() {
        lblConn.setText("Connected ✅");
    }

    @Override
    public void onDisconnected() {
        lblConn.setText("Disconnected ❌");
    }

    @Override
    public void onConnectionError(Exception e) {
        lblConn.setText("Connection error: " + e.getMessage());
    }

    @Override
    public void handleServerMessage(Object msg) {
        // Later: handle LOGIN response here
        // lblMsg.setText("Login response...");
    }
}

