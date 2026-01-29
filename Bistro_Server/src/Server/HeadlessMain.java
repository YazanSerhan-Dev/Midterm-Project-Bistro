package Server;

public class HeadlessMain {

    public static void main(String[] args) {
        int clientPort = 5555; // your client-server port
        // MQTT broker port is NOT opened by us.
        // Your BistroServer connects OUT to tcp://localhost:1883 by default. :contentReference[oaicite:3]{index=3} :contentReference[oaicite:4]{index=4}

        // optional args: --client-port 5555
        for (int i = 0; i < args.length - 1; i++) {
            if ("--client-port".equals(args[i])) {
                clientPort = Integer.parseInt(args[i + 1]);
            }
        }

        try {
            System.out.println("[HEADLESS] Starting BistroServer on client port " + clientPort);
            ServerLauncher.start(clientPort, null); // controller=null => logs go to console :contentReference[oaicite:5]{index=5}
            System.out.println("[HEADLESS] Server is running. Press Ctrl+C to stop.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

