package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the Bistro client.
 * - Reads host/port from command line args.
 * - Loads the ClientView.fxml.
 * - On window close, disconnects from the server so logs show the disconnect.
 */
public class ClientFX extends Application {

    private static String host = "localhost";
    private static int port = 5555;

    private static ClientController controller;
    
    //Possible way to enter the ip via jars

    @Override
    public void init() throws Exception {
        Parameters params = getParameters();
        if (params.getRaw().size() >= 1) {
            host = params.getRaw().get(0);
        }
        if (params.getRaw().size() >= 2) {
            try {
                port = Integer.parseInt(params.getRaw().get(1));
            } catch (NumberFormatException e) {
                System.out.println("Invalid port, using default 5555");
                port = 5555;
            }
        }
    }

    public static String getHost() { return host; }
    public static int getPort() { return port; }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
        Parent root = loader.load();

        // Save controller reference so we can call disconnect on window close
        controller = loader.getController();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Bistro Client - Reservations");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Called when the window is closed (Alt+F4, X button, etc.)
    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.disconnectFromServer();   // will close socket => server logs disconnect
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}






