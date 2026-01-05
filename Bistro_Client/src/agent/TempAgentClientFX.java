package agent;

import Client.BistroClient;
import Client.ClientController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TempAgentClientFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // 1️⃣ Create controller
        ClientController clientController = new ClientController();

        // 2️⃣ Create separate client (NEW SOCKET)
        BistroClient client =
                new BistroClient("localhost", 5555, clientController);

        clientController.setClient(client);

        // 3️⃣ Connect to server
        client.openConnection();

        // 4️⃣ Load Agent UI
        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/agent/AgentView.fxml"));

        Parent root = loader.load();

        AgentController agentController = loader.getController();
        agentController.setClientController(clientController);
        clientController.setAgentController(agentController);

        stage.setTitle("Agent Client (Simulation)");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
