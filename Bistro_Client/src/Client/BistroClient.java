package Client;

import ocsf.client.AbstractClient;

public class BistroClient extends AbstractClient {

    private ClientController controller;

    public BistroClient(String host, int port, ClientController controller) {
        super(host, port);        // call AbstractClient(host, port)
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
    protected void connectionException(Exception exception) {
        controller.onConnectionError(exception);
    }

    @Override
    protected void connectionClosed() {
        controller.onDisconnected();
    }
}


