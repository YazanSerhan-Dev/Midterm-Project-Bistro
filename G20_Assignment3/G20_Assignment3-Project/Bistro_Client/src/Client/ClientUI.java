package Client;
/**
 * Defines the callback interface for client-side UI components.
 * <p>
 * Implementing classes receive connection lifecycle events and
 * messages coming from the server via {@link BistroClient}.
 * </p>
 */
public interface ClientUI {
    /**
     * Called when the client successfully establishes a connection to the server.
     */
    void onConnected();
    /**
     * Called when the connection to the server is closed.
     */
    void onDisconnected();
    /**
     * Called when a connection-related error occurs.
     *
     * @param e the exception describing the connection error
     */
    void onConnectionError(Exception e);
    /**
     * Handles a message received from the server.
     *
     * @param msg the raw message object sent by the server
     */
    void handleServerMessage(Object msg);
}

