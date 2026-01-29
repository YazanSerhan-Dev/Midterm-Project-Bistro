package Server;

public final class ServerLauncher {
    private ServerLauncher() {}

    /**
     * Starts the OCSF server (client port). MQTT is handled inside BistroServer.serverStarted().
     *
     * @param clientPort OCSF port for Java clients (example: 5555)
     * @param controller GUI controller if running with JavaFX, null for headless
     */
    public static BistroServer start(int clientPort, ServerController controller) throws Exception {
        BistroServer server = new BistroServer(clientPort, controller);
        server.listen(); // OCSF listen (THIS is your server port)
        return server;
    }
}
