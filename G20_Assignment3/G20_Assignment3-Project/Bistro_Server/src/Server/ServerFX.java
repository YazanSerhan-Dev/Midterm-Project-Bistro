package Server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * JavaFX entry point for the Bistro Server application.
 *
 * <p>
 * This class is responsible only for:
 * <ul>
 *   <li>Loading the ServerView.fxml file</li>
 *   <li>Creating the main JavaFX window (Stage)</li>
 *   <li>Launching the JavaFX application lifecycle</li>
 * </ul>
 *
 * <p>
 * All server logic (start/stop, client connections, logging)
 * is handled by {@link ServerController} and {@link BistroServer}.
 */
public class ServerFX extends Application {
    /**
     * Called automatically by the JavaFX runtime after launch().
     * Initializes and shows the main server window.
     *
     * @param primaryStage the main application window
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("ServerView.fxml"));
        primaryStage.setTitle("Bistro Server - listening for client connections");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
    /**
     * Application entry point.
     * Starts the JavaFX runtime, which then calls {@link #start(Stage)}.
     */
    public static void main(String[] args) {
        launch(args);
    }
}




