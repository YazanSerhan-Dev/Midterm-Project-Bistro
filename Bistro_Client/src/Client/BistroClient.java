package Client;

import javafx.application.Platform;
import ocsf.client.AbstractClient;

/**
 * Simple OCSF client that delegates events to the ClientController.
 */
public class BistroClient extends AbstractClient {

    private final ClientController controller;

    /**
     * Creates a new client connected to the given host/port and bound to a controller.
     */
    public BistroClient(String host, int port, ClientController controller) {
        super(host, port);
        this.controller = controller;
    }

    @Override
    protected void connectionEstablished() {
        Platform.runLater(() -> controller.onConnected());
    }

    @Override
    protected void connectionClosed() {
        Platform.runLater(() -> controller.onDisconnected());
    }

    @Override
    protected void connectionException(Exception exception) {
        Platform.runLater(() -> controller.onConnectionError(exception));
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        Platform.runLater(() -> controller.handleServerMessage(msg));
        
    }
    
}



