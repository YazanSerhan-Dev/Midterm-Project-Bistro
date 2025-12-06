package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientFX extends Application {

    private ClientController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
        Scene scene = new Scene(loader.load());

        controller = loader.getController();

        primaryStage.setTitle("Bistro Client - Reservations");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // when window is closed, disconnect from server
        primaryStage.setOnCloseRequest(e -> controller.disconnectFromServer());
    }

    public static void main(String[] args) {
        launch(args);
    }
}






