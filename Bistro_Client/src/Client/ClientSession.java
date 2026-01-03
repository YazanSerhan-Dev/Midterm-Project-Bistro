package Client;

public class ClientSession {

    private static BistroClient client;
    private static String host = "localhost";
    private static int port = 5555;

    public static void configure(String host, int port) {
        ClientSession.host = host;
        ClientSession.port = port;
    }

    public static BistroClient getClient() {
        return client;
    }

    // Connect once (or reuse if already connected)
    public static void connect(ClientUI ui) throws Exception {
        if (client == null) {
            client = new BistroClient(host, port, ui);
            client.openConnection();
            return;
        }

        // reuse existing client, just swap who receives callbacks
        client.setUI(ui);

        if (!client.isConnected()) {
            client.openConnection();
        }
    }

    public static void bindUI(ClientUI ui) {
        if (client != null) {
            client.setUI(ui);
        }
    }

    public static void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();
            }
        } catch (Exception ignored) {}
    }
}

