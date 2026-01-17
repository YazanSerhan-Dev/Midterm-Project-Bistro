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

    /**
     * JavaFX initialization hook invoked by the FXMLLoader after all @FXML fields are injected.
     * <p>
     * Responsibilities:
     * <ul>
     *   <li>Binds this controller to {@link ClientSession} so server callbacks are routed here.</li>
     *   <li>Clears any previous UI message.</li>
     *   <li>Allows pressing ENTER in the QR text field to trigger subscriber continuation.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        ClientSession.bindUI(this);

        if (lblMsg != null) lblMsg.setText("");

        if (txtSubscriberQR != null) {
            txtSubscriberQR.setOnAction(e -> onSubscriberContinue());
        }
    }
    
    
    /**
     * Continues into the terminal as a customer (no subscriber identification).
     * <p>
     * This method clears any subscriber identity stored in {@link ClientSession} and navigates
     * to the terminal in customer mode.
     */
    @FXML
    private void onCustomer() {
        // Clear any subscriber identity
        ClientSession.setRole("CUSTOMER");
        ClientSession.setUsername("");
        SceneManager.showTerminal(false);
    }
      
    /**
     * Continues into the terminal as a subscriber using the subscriber QR code.
     * <p>
     * Validates that:
     * <ul>
     *   <li>A QR code was entered/scanned.</li>
     *   <li>The client is connected to the server.</li>
     * </ul>
     * Sends {@link OpCode#REQUEST_TERMINAL_RESOLVE_SUBSCRIBER_QR} with the QR value to the server.
     * <p>
     * Uses {@code resolveInFlight} to prevent duplicate requests while a previous request is pending.
     */
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
    
    /**
     * Shows or hides the username/password login fallback panel for subscribers who forgot their QR.
     * <p>
     * When opened, this method focuses the username field for faster input.
     * Also clears any message label text.
     */
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

    /**
     * Attempts to authenticate a subscriber using username and password (fallback flow).
     * <p>
     * Validates that both username and password are provided, then sends
     * {@link OpCode#REQUEST_LOGIN_SUBSCRIBER} with a {@link LoginRequestDTO} payload.
     * Disables the login button until a response is received to prevent duplicate submits.
     */
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
    /**
     * Callback invoked when the client successfully connects to the server.
     * <p>
     * Currently no special UI handling is required for this screen.
     */
    @Override public void onConnected() {}
    
    /**
     * Callback invoked when the client disconnects from the server.
     * <p>
     * Resets any in-flight QR resolve request and re-enables the subscriber continue button
     * so the UI does not remain stuck in a disabled state.
     * Runs on the JavaFX Application Thread via {@link Platform#runLater(Runnable)}.
     */
    @Override public void onDisconnected() {
        Platform.runLater(() -> {
            resolveInFlight = false;
            if (btnSubscriberContinue != null) btnSubscriberContinue.setDisable(false);
        });
    }
    
    /**
     * Callback invoked when the connection fails or an I/O error occurs.
     * <p>
     * Resets any in-flight QR resolve request and re-enables the subscriber continue button.
     * Runs on the JavaFX Application Thread via {@link Platform#runLater(Runnable)}.
     *
     * @param e the underlying connection error.
     */
    @Override public void onConnectionError(Exception e) {
        Platform.runLater(() -> {
            resolveInFlight = false;
            if (btnSubscriberContinue != null) btnSubscriberContinue.setDisable(false);
        });
    }

    /**
     * Receives server messages routed to this controller and updates the UI accordingly.
     * <p>
     * This method unwraps the incoming object into an {@link Envelope} (either directly or from a
     * {@link KryoMessage}) and then handles specific response opcodes:
     * <ul>
     *   <li>{@link OpCode#RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR}: resolves subscriber identity from a QR code.</li>
     *   <li>{@link OpCode#RESPONSE_LOGIN_SUBSCRIBER}: resolves subscriber identity from username/password login.</li>
     * </ul>
     * <p>
     * On successful subscriber resolution, the method stores subscriber identity (and contact info, when provided)
     * into {@link ClientSession} so that the terminal screen can restore subscriber mode across navigation.
     * All UI work is executed on the JavaFX Application Thread via {@link Platform#runLater(Runnable)}.
     *
     * @param message the raw message object received from the networking layer.
     */
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
    /**
     * Checks whether the client networking layer is currently connected to the server.
     *
     * @return {@code true} if a client instance exists and is connected; otherwise {@code false}.
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
     * Converts a raw network message into an {@link Envelope}.
     * <p>
     * Supported formats:
     * <ul>
     *   <li>{@link Envelope} directly.</li>
     *   <li>{@link KryoMessage} of type {@code "ENVELOPE"} whose payload is a serialized {@link Envelope}.</li>
     * </ul>
     *
     * @param msg raw message object received from the server/client networking layer.
     * @return the decoded {@link Envelope}, or {@code null} if the message is not an envelope or cannot be decoded.
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
}
