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

        // choose your default host/port here
        ClientSession.configure("localhost", 5555);

        // first scene = login
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









