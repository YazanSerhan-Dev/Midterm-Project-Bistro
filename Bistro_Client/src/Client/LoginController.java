package Client;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import common.dto.LoginResponseDTO;


public class LoginController implements ClientUI {

    @FXML private Label lblConn;
    @FXML private Label lblMsg;
    @FXML private TextField tfUsername;
    @FXML private PasswordField pfPassword;

    @FXML
    public void initialize() {
    	onConnected();
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

        try {
            BistroClient c = ClientSession.getClient();
            if (c == null || !c.isConnected()) {
                lblMsg.setText("Not connected to server.");
                return;
            }

            common.dto.LoginRequestDTO dto = new common.dto.LoginRequestDTO(user.trim(), pass);
            Envelope env = Envelope.request(OpCode.REQUEST_LOGIN_SUBSCRIBER, dto);

            c.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblMsg.setText("Signing in as subscriber...");
        } catch (Exception ex) {
            lblMsg.setText("Send failed: " + ex.getMessage());
            ex.printStackTrace();
        }
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

        try {
            BistroClient c = ClientSession.getClient();
            if (c == null || !c.isConnected()) {
                lblMsg.setText("Not connected to server.");
                return;
            }

            common.dto.LoginRequestDTO dto = new common.dto.LoginRequestDTO(user.trim(), pass);
            Envelope env = Envelope.request(OpCode.REQUEST_LOGIN_STAFF, dto);

            c.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));

            lblMsg.setText("Signing in as staff...");
        } catch (Exception ex) {
            lblMsg.setText("Send failed: " + ex.getMessage());
            ex.printStackTrace();
        }
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
    
    // ====== Networking ======
    private Envelope unwrapToEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope e) return e;

            if (msg instanceof KryoMessage km) {
                Object obj = KryoUtil.fromBytes(km.getPayload());
                if (obj instanceof Envelope e) return e;
            }
        } catch (Exception ex) {
            lblMsg.setText("Decode error: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public void handleServerMessage(Object msg) {
        Platform.runLater(() -> {
            Envelope env = unwrapToEnvelope(msg);
            if (env == null) {
                lblMsg.setText("Decode failed.");
                return;
            }

            switch (env.getOp()) {

            case RESPONSE_LOGIN_SUBSCRIBER -> {
                LoginResponseDTO res = (LoginResponseDTO) env.getPayload();

                if (res.isOk()) {
                    ClientSession.setRole("SUBSCRIBER");
                    ClientSession.setUsername(res.getUsername());

                    // ✅ THIS IS THE MISSING LINE
                    ClientSession.setMemberCode(res.getMemberCode());

                    // (optional but recommended debug)
                    System.out.println("LOGIN SUBSCRIBER: username=" + res.getUsername()
                            + " memberCode=" + res.getMemberCode());

                    lblMsg.setText("");
                    SceneManager.showCustomerMain();
                } else {
                    lblMsg.setText(res.getMessage());
                }
            }


                case RESPONSE_LOGIN_STAFF -> {
                    LoginResponseDTO res = (LoginResponseDTO) env.getPayload();

                    if (res.isOk()) {
                        // role from DB: "AGENT" or "MANAGER"
                        ClientSession.setRole(res.getRole());
                        ClientSession.setUsername(res.getUsername());

                        lblMsg.setText("");
                        SceneManager.showStaffDashboard();
                    } else {
                        lblMsg.setText(res.getMessage());
                    }
                }

                default -> {
                    // ignore other messages while on login screen
                }
            }
        });
    }

}


