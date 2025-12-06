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

    /**
     * Initializes table bindings and sets initial UI state (server stopped).
     */
    @FXML
    private void initialize() {
        hostColumn.setCellValueFactory(c -> c.getValue().hostNameProperty());
        ipColumn.setCellValueFactory(c -> c.getValue().ipAddressProperty());
        statusColumn.setCellValueFactory(c -> c.getValue().statusProperty());

        clientsTable.setItems(clientRows);

        setServerStoppedUI();
    }

    /* ========== Button handlers ========== */

    /**
     * Starts the server on the default port and updates the UI.
     */
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

    /**
     * Stops the server if it is running and updates the UI.
     */
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

    /**
     * Exits the application, closing the server if needed.
     */
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

    /**
     * Updates the UI when the server has started (callback from BistroServer).
     */
    public void onServerStarted(int port) {
        setServerStartedUI(port);
    }

    /**
     * Updates the UI and clears the connections table when the server stops.
     */
    public void onServerStopped() {
        setServerStoppedUI();
        Platform.runLater(() -> clientRows.clear());
    }

    /**
     * Adds a new row when a client connects.
     */
    public void onClientConnected(String hostName, String ipAddress) {
        Platform.runLater(() -> {
            clientRows.add(new ClientConnectionRow(hostName, ipAddress, "connected"));
            clientsTable.refresh();
        });
    }

    /**
     * Marks the last active client row as disconnected when a client disconnects.
     */
    public void onClientDisconnected(String hostName, String ipAddress) {
        Platform.runLater(() -> {
            if (clientRows.isEmpty()) {
                return;
            }

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

    /**
     * Appends a log line to the server log area (thread-safe).
     */
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

    /**
     * Sets labels and buttons for the "server started" state.
     */
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

    /**
     * Sets labels and buttons for the "server stopped" state.
     */
    private void setServerStoppedUI() {
        Platform.runLater(() -> {
            serverStatusLabel.setText("Server is stopped");
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });
    }
}







