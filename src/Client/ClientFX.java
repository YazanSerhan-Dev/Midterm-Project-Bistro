package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientFX extends Application {

    private static String host = "localhost";
    private static int port = 5555;

    @Override
    public void init() throws Exception {
        Parameters params = getParameters();
        if (params.getRaw().size() >= 1) {
            host = params.getRaw().get(0);          // IP from command line
        }
        if (params.getRaw().size() >= 2) {
            port = Integer.parseInt(params.getRaw().get(1));   // optional port
        }
    }

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Bistro Client - Reservations");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // ✅ Check that an IP was passed
        if (args.length < 1) {
            System.out.println("No IP address provided.");
            System.out.println("Usage: java -jar Client.jar <server-ip> [port]");
            return;
        }

        launch(args);
    }
}



