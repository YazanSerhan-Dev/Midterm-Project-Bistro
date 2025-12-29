package Client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    private static void setRoot(String fxmlFileName, String title) {
        try {
            // Since all FXML files are in the same resources folder as ClientFX,
            // we load them by name.
            Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlFileName));
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlFileName, e);
        }
    }

    public static void showCustomerMain() {
        setRoot("ClientView.fxml", "Bistro Client - Customer/Subscriber");
    }

    public static void showTerminal() {
        setRoot("TerminalView.fxml", "Bistro Client - Terminal");
    }

    public static void showPayBill() {
        setRoot("PayBillView.fxml", "Bistro Client - Pay Bill");
    }

    public static void showLogin() {
        setRoot("LoginView.fxml", "Bistro Client - Login");
    }
}


