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
 * JavaFX controller for the server window (FXML).
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Start/stop the {@link BistroServer} (OCSF server) on the default port.</li>
 *   <li>Display server status (IP:port) and enable/disable UI buttons accordingly.</li>
 *   <li>Maintain the connected-clients table using {@link ClientConnectionRow}.</li>
 *   <li>Append runtime logs to the UI log area in a thread-safe way.</li>
 * </ul>
 * <p>
 * Threading:
 * <ul>
 *   <li>{@link BistroServer} callbacks may arrive from a non-JavaFX thread.</li>
 *   <li>All UI updates are wrapped with {@link Platform#runLater(Runnable)}.</li>
 * </ul>
 */
public class ServerController {

    /** Label that shows whether the server is running and on which IP/port. */
    @FXML private Label serverStatusLabel;

    @FXML private TableView<ClientConnectionRow> clientsTable;
    @FXML private TableColumn<ClientConnectionRow, String> hostColumn;
    @FXML private TableColumn<ClientConnectionRow, String> ipColumn;
    @FXML private TableColumn<ClientConnectionRow, String> statusColumn;
    /** Text area used as a live server log console. */
    @FXML private TextArea logArea;
    /** UI controls for server lifecycle. */
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
     * JavaFX initialization hook called after FXML injection.
     * Binds table columns to {@link ClientConnectionRow} properties and initializes UI state.
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
     * Starts the server if it is not already running.
     * Creates a new {@link BistroServer} instance and calls {@code listen()} on the default port.
     * Logs success/failure to the log area.
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
     * Stops the server if it is currently listening.
     * Updates UI state and logs the result.
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
     * Exits the application.
     * Attempts to close the server (if exists) and then terminates JavaFX.
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
     * Callback invoked when the server has started listening.
     *
     * @param port the port the server is listening on
     */
    public void onServerStarted(int port) {
        setServerStartedUI(port);
    }
    /**
     * Callback invoked when the server has stopped.
     * Clears the clients table to reflect that there are no active connections.
     */
    public void onServerStopped() {
        setServerStoppedUI();
        Platform.runLater(clientRows::clear);
    }
    /**
     * Adds a new "connected" row to the clients table for a newly connected client.
     *
     * @param hostName client machine host name
     * @param ipAddress client IP address
     */
    public void onClientConnected(String hostName, String ipAddress) {
        Platform.runLater(() -> {
            clientRows.add(new ClientConnectionRow(hostName, ipAddress, "connected"));
            clientsTable.refresh();
        });
    }
    /**
     * Marks an existing client row as "disconnected".
     * If the matching row is not found, the table is still refreshed as a safe fallback.
     *
     * @param hostName client machine host name
     * @param ipAddress client IP address
     */
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

    /**
     * Appends a log message to the UI log area in a thread-safe manner.
     * If {@link #logArea} is not available (should not happen in normal UI), prints to stdout.
     *
     * @param msg log message to append
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
     * Updates UI to reflect "server started" state:
     * sets the status label to "listening on IP:port" and toggles start/stop buttons.
     *
     * @param port the port shown in the UI
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
     * Updates UI to reflect "server stopped" state:
     * sets the status label and toggles start/stop buttons.
     */
    private void setServerStoppedUI() {
        Platform.runLater(() -> {
            serverStatusLabel.setText("Server is stopped");
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });
    }
}







