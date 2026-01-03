package Client;

public interface ClientUI {
    void onConnected();
    void onDisconnected();
    void onConnectionError(Exception e);
    void handleServerMessage(Object msg);
}

