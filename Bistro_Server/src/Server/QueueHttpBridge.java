package Server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class QueueHttpBridge {

    public interface QueueStatsProvider {
        int getWaiting();
        int getArrived();
        int getCompleted();
    }

    private final int port;
    private final QueueStatsProvider provider;
    private HttpServer server;

    public QueueHttpBridge(int port, QueueStatsProvider provider) {
        this.port = port;
        this.provider = provider;
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/queue/status", new StatusHandler(provider));
        server.setExecutor(null);
        server.start();

        System.out.println("[QueueHttpBridge] http://0.0.0.0:" + port + "/queue/status");
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private static class StatusHandler implements HttpHandler {
        private final QueueStatsProvider provider;

        StatusHandler(QueueStatsProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handle(HttpExchange ex) {
            try {
                if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                    ex.sendResponseHeaders(405, -1);
                    return;
                }

                int W = provider.getWaiting();
                int A = provider.getArrived();
                int C = provider.getCompleted();

                String body = "W:" + W + " A:" + A + " C:" + C;
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

                ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                ex.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = ex.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                try { ex.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
            }
        }
    }
}
