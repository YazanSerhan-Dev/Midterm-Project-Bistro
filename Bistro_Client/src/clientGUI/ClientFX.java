package clientGUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

import Client.ClientController;
import Client.SceneManager;

public class ClientFX extends Application {

    private ClientController mainController;

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Parse args from: java -jar Client.jar <host> <port>
        List<String> args = getParameters().getRaw();
        String host = args.size() >= 1 ? args.get(0) : "127.0.0.1";
        int port = args.size() >= 2 ? safeParsePort(args.get(1), 5555) : 5555;

        SceneManager.init(primaryStage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
        Scene scene = new Scene(loader.load());

        mainController = loader.getController();

        primaryStage.setTitle("Bistro Client - Customer/Subscriber");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Auto-connect (no GUI fields)
        mainController.connectToServer(host, port);

        primaryStage.setOnCloseRequest(e -> {
            if (mainController != null) {
                mainController.disconnectFromServer();
            }
        });
    }

    private int safeParsePort(String s, int def) {
        try {
            int p = Integer.parseInt(s.trim());
            return (p >= 1 && p <= 65535) ? p : def;
        } catch (Exception e) {
            return def;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}









