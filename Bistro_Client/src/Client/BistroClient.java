package Client;

import javafx.application.Platform;
import ocsf.client.AbstractClient;

public class BistroClient extends AbstractClient {

    private ClientUI ui;

    public BistroClient(String host, int port, ClientUI ui) {
        super(host, port);
        this.ui = ui;
    }

    public void setUI(ClientUI ui) {
        this.ui = ui;
    }

    @Override
    protected void connectionEstablished() {
        Platform.runLater(() -> { if (ui != null) ui.onConnected(); });
    }

    @Override
    protected void connectionClosed() {
        Platform.runLater(() -> { if (ui != null) ui.onDisconnected(); });
    }

    @Override
    protected void connectionException(Exception exception) {
        Platform.runLater(() -> { if (ui != null) ui.onConnectionError(exception); });
    }

@Override
protected void handleMessageFromServer(Object msg) {
    Platform.runLater(() -> {
        if (ui != null) {
            ui.handleServerMessage(msg);
        }
    });
}

    
}




