package Client;

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

    /**
     * Called when a message arrives from the server; forwards it to the controller.
     */
    @Override
    protected void handleMessageFromServer(Object msg) {
        controller.handleServerMessage(msg);
    }

    /**
     * Called when the TCP connection is successfully established.
     */
    @Override
    protected void connectionEstablished() {
        controller.onConnected();
    }

    /**
     * Called when the TCP connection is closed (by client or server).
     */
    @Override
    protected void connectionClosed() {
        controller.onDisconnected();
    }

    /**
     * Called when a connection-related exception occurs.
     */
    @Override
    protected void connectionException(Exception exception) {
        controller.onConnectionError(exception);
    }
}



