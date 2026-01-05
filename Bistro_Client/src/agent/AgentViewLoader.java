package agent;

import Client.ClientController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AgentViewLoader {

    public static void open(ClientController clientController) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(AgentViewLoader.class
                            .getResource("/agent/AgentView.fxml"));

            Parent root = loader.load();

            AgentController agentController = loader.getController();
            agentController.setClientController(clientController);

            Stage stage = new Stage();
            stage.setTitle("Agent Panel");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
