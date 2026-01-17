package Client;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.LoginRequestDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class TerminalEntryController implements ClientUI {

    @FXML private Button btnCustomer;
    @FXML private Button btnSubscriberContinue;
    @FXML private TextField txtSubscriberQR;
    @FXML private Label lblMsg;
    @FXML private VBox loginBox;
    @FXML private TextField txtSubUsername;
    @FXML private PasswordField txtSubPassword;
    @FXML private Button btnLoginSubscriber;

    private volatile boolean resolveInFlight = false;

    @FXML
    private void initialize() {
        ClientSession.bindUI(this);

        if (lblMsg != null) lblMsg.setText("");

        if (txtSubscriberQR != null) {
            txtSubscriberQR.setOnAction(e -> onSubscriberContinue());
        }
    }

    @FXML
    private void onCustomer() {
        // Clear any subscriber identity
        ClientSession.setRole("CUSTOMER");
        ClientSession.setUsername("");
        SceneManager.showTerminal(false);
    }

    @FXML
    private void onSubscriberContinue() {
        if (resolveInFlight) return;
        if (lblMsg != null) lblMsg.setText("");

        String qr = (txtSubscriberQR == null) ? "" : txtSubscriberQR.getText().trim();
        if (qr.isBlank()) {
            if (lblMsg != null) lblMsg.setText("Please enter/scan your subscriber QR code.");
            return;
        }

        if (!isConnected()) {
            if (lblMsg != null) lblMsg.setText("Not connected to server.");
            return;
        }

        resolveInFlight = true;
        btnSubscriberContinue.setDisable(true);

        try {
            Envelope env = Envelope.request(OpCode.REQUEST_TERMINAL_RESOLVE_SUBSCRIBER_QR, qr);
            ClientSession.getClient().sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));
        } catch (Exception ex) {
            resolveInFlight = false;
            btnSubscriberContinue.setDisable(false);
            if (lblMsg != null) lblMsg.setText("Failed to send: " + ex.getMessage());
        }
    }
    
    @FXML
    private void onToggleLogin() {
        boolean show = !loginBox.isVisible();
        loginBox.setVisible(show);
        loginBox.setManaged(show);

        if (lblMsg != null) lblMsg.setText("");

        if (show) {
            txtSubUsername.requestFocus();
        }
    }

    @FXML
    private void onLoginSubscriber() {
        if (lblMsg != null) lblMsg.setText("");

        String username = txtSubUsername.getText().trim();
        String password = txtSubPassword.getText();

        if (username.isBlank() || password.isBlank()) {
            lblMsg.setText("Please enter username and password.");
            return;
        }

        try {
            LoginRequestDTO req = new LoginRequestDTO(username, password);
            Envelope env = Envelope.request(OpCode.REQUEST_LOGIN_SUBSCRIBER, req);

            ClientSession.getClient().sendToServer(
                new KryoMessage("ENVELOPE", KryoUtil.toBytes(env))
            );

            btnLoginSubscriber.setDisable(true);

        } catch (Exception e) {
            btnLoginSubscriber.setDisable(false);
            lblMsg.setText("Failed to send login request.");
        }
    }

    // ===== ClientUI =====
    @Override public void onConnected() {}
    @Override public void onDisconnected() {
        Platform.runLater(() -> {
            resolveInFlight = false;
            if (btnSubscriberContinue != null) btnSubscriberContinue.setDisable(false);
        });
    }
    @Override public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            resolveInFlight = false;
            if (btnSubscriberContinue != null) btnSubscriberContinue.setDisable(false);
        });
    }

    @Override
    public void handleServerMessage(Object message) {
        Platform.runLater(() -> {

            Envelope env = unwrapEnvelope(message);
            if (env == null) return;

            // ===========================
            // 1) QR resolve subscriber
            // ===========================
            if (env.getOp() == OpCode.RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR) {
                resolveInFlight = false;
                if (btnSubscriberContinue != null) btnSubscriberContinue.setDisable(false);

                Object payload = env.getPayload();

                // error string
                if (payload instanceof String msg) {
                    if (lblMsg != null) lblMsg.setText(msg);
                    return;
                }

                // expected Object[] { username, email, phone, list }
                if (payload instanceof Object[] arr && arr.length >= 4) {

                    String username = String.valueOf(arr[0]);
                    String email    = arr[1] == null ? "" : String.valueOf(arr[1]);
                    String phone    = arr[2] == null ? "" : String.valueOf(arr[2]);

                    // ✅ store identity globally (survives navigation)
                    ClientSession.setRole("SUBSCRIBER");
                    ClientSession.setUsername(username);

                    // ✅ IMPORTANT: store subscriber contact so Terminal can auto-fill recover
                    ClientSession.setSubscriberEmail(email);
                    ClientSession.setSubscriberPhone(phone);

                    // open terminal in subscriber mode
                    SceneManager.showTerminal(true);
                    return;
                }

                // backward compatible old format Object[] { username, list }
                if (payload instanceof Object[] arr && arr.length >= 2) {
                    String username = String.valueOf(arr[0]);

                    ClientSession.setRole("SUBSCRIBER");
                    ClientSession.setUsername(username);

                    // no contact in old format -> Terminal won't autofill
                    ClientSession.setSubscriberEmail("");
                    ClientSession.setSubscriberPhone("");

                    SceneManager.showTerminal(true);
                    return;
                }

                if (lblMsg != null) lblMsg.setText("Unexpected response from server.");
            }

            // ===========================
            // 2) Subscriber login fallback
            // ===========================
            if (env.getOp() == OpCode.RESPONSE_LOGIN_SUBSCRIBER) {

                if (btnLoginSubscriber != null) btnLoginSubscriber.setDisable(false);

                Object payload = env.getPayload();
                if (!(payload instanceof common.dto.LoginResponseDTO res)) {
                    if (lblMsg != null) lblMsg.setText("Invalid login response.");
                    return;
                }

                if (!res.isOk()) {
                    if (lblMsg != null) lblMsg.setText(res.getMessage());
                    return;
                }

                // ✅ store identity globally
                ClientSession.setRole(res.getRole());          // should be "SUBSCRIBER"
                ClientSession.setUsername(res.getUsername());
                ClientSession.setMemberCode(res.getMemberCode());

                // optional UX cleanup
                if (loginBox != null) {
                    loginBox.setVisible(false);
                    loginBox.setManaged(false);
                }

                SceneManager.showTerminal(true);
            }
        });
    }


    // ===== helpers =====
    private boolean isConnected() {
        try {
            var c = ClientSession.getClient();
            return c != null && c.isConnected();
        } catch (Exception e) {
            return false;
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
}
