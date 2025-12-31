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

/**
 * JavaFX controller for the server window.
 * Manages server lifecycle, client connections table, and log area.
 */
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

    /** The OCSF server instance managed by this controller. */
    private BistroServer server;

    /** Observable list backing the clients table. */
    private final ObservableList<ClientConnectionRow> clientRows =
            FXCollections.observableArrayList();

    private static final int DEFAULT_PORT = 5555;

    @FXML
    private void initialize() {
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
        Platform.runLater(clientRows::clear);
    }

    public void onClientConnected(String hostName, String ipAddress) {
        Platform.runLater(() -> {
            clientRows.add(new ClientConnectionRow(hostName, ipAddress, "connected"));
            clientsTable.refresh();
        });
    }

    public void onClientDisconnected(String hostName, String ipAddress) {
        Platform.runLater(() -> {
            for (int i = clientRows.size() - 1; i >= 0; i--) {
                ClientConnectionRow row = clientRows.get(i);

                boolean sameClient =
                        hostName.equals(row.getHostName()) &&
                        ipAddress.equals(row.getIpAddress());

                if (sameClient && "connected".equalsIgnoreCase(row.getStatus())) {
                    row.setStatus("disconnected");
                    clientsTable.refresh();
                    return;
                }
            }

            // fallback: if not found, still refresh
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







