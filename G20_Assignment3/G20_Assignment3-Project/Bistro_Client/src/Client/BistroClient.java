package Client;

import javafx.application.Platform;
import ocsf.client.AbstractClient;
/**
 * Client-side network component of the Bistro system.
 * <p>
 * This class extends {@link AbstractClient} and is responsible for
 * managing the connection to the server and forwarding connection
 * events and messages to the client UI.
 * </p>
 */
public class BistroClient extends AbstractClient {

    private ClientUI ui;
    /**
     * Creates a new BistroClient and initializes the connection parameters.
     *
     * @param host the server IP address
     * @param port the server port number
     * @param ui   the client UI handler used to update the interface
     */
    public BistroClient(String host, int port, ClientUI ui) {
        super(host, port);
        this.ui = ui;
    }

    /**
     * Sets or updates the client UI handler.
     *
     * @param ui the UI instance to be associated with this client
     */

    public void setUI(ClientUI ui) {
        this.ui = ui;
    }
    /**
     * Called automatically when a connection to the server is established.
     * Notifies the UI on the JavaFX Application Thread.
     */
    @Override
    protected void connectionEstablished() {
        Platform.runLater(() -> { if (ui != null) ui.onConnected(); });
    }
    /**
     * Called automatically when the connection to the server is closed.
     * Notifies the UI on the JavaFX Application Thread.
     */
    @Override
    protected void connectionClosed() {
        Platform.runLater(() -> { if (ui != null) ui.onDisconnected(); });
    }
    /**
     * Called automatically when a connection error occurs.
     * Notifies the UI with the received exception.
     *
     * @param exception the exception describing the connection error
     */
    @Override
    protected void connectionException(Exception exception) {
        Platform.runLater(() -> { if (ui != null) ui.onConnectionError(exception); });
    }

    /**
     * Handles messages received from the server and forwards them to the UI.
     *
     * @param msg the message received from the server
     */
@Override
protected void handleMessageFromServer(Object msg) {
    Platform.runLater(() -> {
        if (ui != null) {
            ui.handleServerMessage(msg);
        }
    });
}

    
}




