package Server;

import java.io.IOException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

public class ServerController {

    @FXML private Label serverStatusLabel;

    @FXML private TableView<ClientConnectionRow> clientsTable;
    @FXML private TableColumn<ClientConnectionRow, String> hostColumn;
    @FXML private TableColumn<ClientConnectionRow, String> ipColumn;
    @FXML private TableColumn<ClientConnectionRow, String> statusColumn;

    @FXML private TextArea logArea;

    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button exitButton;

    private BistroServer server;

    private final ObservableList<ClientConnectionRow> clientRows =
            FXCollections.observableArrayList();

    private static final int DEFAULT_PORT = 5555;

    @FXML
    private void initialize() {
        // bind columns to properties in ClientConnectionRow
        hostColumn.setCellValueFactory(c -> c.getValue().hostNameProperty());
        ipColumn.setCellValueFactory(c -> c.getValue().ipAddressProperty());
        statusColumn.setCellValueFactory(c -> c.getValue().statusProperty());

        clientsTable.setItems(clientRows);

        setServerStoppedUI();
    }

    /* ========== Button handlers ========== */

    @FXML
    private void onStartServer() {
        if (server != null && server.isListening()) {
            appendLogFromServer("Server is already running.");
            return;
        }

        try {
            server = new BistroServer(DEFAULT_PORT, this);
            server.listen();
            setServerStartedUI(DEFAULT_PORT);
            appendLogFromServer("Server started on port " + DEFAULT_PORT);
        } catch (IOException e) {
            appendLogFromServer("Failed to start server: " + e.getMessage());
        }
    }

    @FXML
    private void onStopServer() {
        if (server == null || !server.isListening()) {
            appendLogFromServer("Server is not running.");
            return;
        }

        try {
            server.close();
            setServerStoppedUI();
            appendLogFromServer("Server stopped.");
        } catch (IOException e) {
            appendLogFromServer("Failed to stop server: " + e.getMessage());
        }
    }

    @FXML
    private void onExit() {
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException ignored) { }
        Platform.exit();
    }

    /* ========== Called from BistroServer (server thread) ========== */

    public void onServerStarted(int port) {
        setServerStartedUI(port);
    }

    public void onServerStopped() {
        setServerStoppedUI();
        // נקה את הטבלה תמיד דרך ה-FX thread
        Platform.runLater(() -> clientRows.clear());
    }

    // נקרא מה-BistroServer כל פעם שקליינט מתחבר
    public void onClientConnected(String hostName, String ipAddress) {
        Platform.runLater(() -> {
            // כל חיבור חדש = שורה חדשה עם connected
            clientRows.add(new ClientConnectionRow(hostName, ipAddress, "connected"));
            clientsTable.refresh();
        });
    }

    // נקרא מה-BistroServer כל פעם שקליינט מתנתק / נזרקת Exception
    public void onClientDisconnected(String hostName, String ipAddress) {
        Platform.runLater(() -> {
            if (clientRows.isEmpty()) {
                return;
            }

            // רעיון פשוט:
            // כל ניתוק מסמן את *השורה האחרונה שעדיין לא מסומנת כ-Disconnected*
            for (int i = clientRows.size() - 1; i >= 0; i--) {
                ClientConnectionRow row = clientRows.get(i);
                if (!"Disconnected".equalsIgnoreCase(row.getStatus())) {
                    row.setStatus("Disconnected");
                    break;
                }
            }

            clientsTable.refresh();
        });
    }



    public void appendLogFromServer(String msg) {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(msg + System.lineSeparator());
            } else {
                System.out.println(msg);
            }
        });
    }

    /* ========== UI helpers ========== */

    private void setServerStartedUI(int port) {
        Platform.runLater(() -> {
            String ip = "unknown";
            try {
                ip = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignored) { }

            serverStatusLabel.setText(
                    "Server is listening for connections on " + ip + ":" + port + "..."
            );

            startButton.setDisable(true);
            stopButton.setDisable(false);
        });
    }

    private void setServerStoppedUI() {
        Platform.runLater(() -> {
            serverStatusLabel.setText("Server is stopped");
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });
    }
}






