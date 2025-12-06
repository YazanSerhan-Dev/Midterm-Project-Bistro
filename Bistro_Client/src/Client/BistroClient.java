package Client;

import ocsf.client.AbstractClient;

/**
 * Simple OCSF client.
 * Forwards events to ClientController.
 */
public class BistroClient extends AbstractClient {

    private final ClientController controller;

    public BistroClient(String host, int port, ClientController controller) {
        super(host, port);
        this.controller = controller;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        controller.handleServerMessage(msg);
    }

    @Override
    protected void connectionEstablished() {
        controller.onConnected();
    }

    @Override
    protected void connectionClosed() {
        controller.onDisconnected();
    }

    @Override
    protected void connectionException(Exception exception) {
        controller.onConnectionError(exception);
    }
}


