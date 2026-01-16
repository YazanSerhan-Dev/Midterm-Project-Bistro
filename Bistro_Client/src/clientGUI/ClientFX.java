package clientGUI;

import javafx.application.Application;

import javafx.stage.Stage;

import Client.ClientController;
import Client.ClientSession;
import Client.SceneManager;

public class ClientFX extends Application {

    private ClientController mainController;

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);

        // defaults
        String host = "localhost";
        int port = 5555;

        // read args: java -jar client.jar 192.168.1.42 5555
        var args = getParameters().getRaw();
        if (args.size() >= 1) host = args.get(0);
        if (args.size() >= 2) port = safeParsePort(args.get(1), 5555);

        ClientSession.configure(host, port);

        SceneManager.showLogin();
        primaryStage.setOnCloseRequest(e -> ClientSession.disconnect());
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









