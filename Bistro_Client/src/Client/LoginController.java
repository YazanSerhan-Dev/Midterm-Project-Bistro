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
        lblConn.setText("Connecting...");
        lblMsg.setText("");

        try {
            ClientSession.connect(this); // same shared connection for the whole app
        } catch (Exception e) {
            lblConn.setText("Connection failed: " + e.getMessage());
        }
    }

    // ===== Buttons =====

    @FXML
    public void onLoginSubscriber(ActionEvent e) {
        lblMsg.setText("");

        String user = tfUsername.getText();
        String pass = pfPassword.getText();

        if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
            lblMsg.setText("Please enter username and password.");
            return;
        }

        // TODO: send real login request to server
        // for now route directly:
        ClientSession.setRole("SUBSCRIBER");
        ClientSession.setUsername(user);

        SceneManager.showCustomerMain();
    }

    @FXML
    public void onLoginAgent(ActionEvent e) {
        lblMsg.setText("");

        String user = tfUsername.getText();
        String pass = pfPassword.getText();

        if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
            lblMsg.setText("Please enter username and password.");
            return;
        }

        // TODO: send real agent login request to server
        ClientSession.setRole("AGENT");
        ClientSession.setUsername(user);

        SceneManager.showTerminal();
    }

    @FXML
    public void onContinueCustomer(ActionEvent e) {
        lblMsg.setText("");

        // no login needed
        ClientSession.setRole("CUSTOMER");
        ClientSession.setUsername("customer");

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
        // Later: handle LOGIN responses here
    }
}


