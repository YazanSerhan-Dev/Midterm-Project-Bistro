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

    private static void setRoot(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            // ✅ ADD THIS BLOCK (bind the active controller to the shared client)
            Object controller = loader.getController();
            if (controller instanceof ClientUI ui) {
                ClientSession.bindUI(ui);
            }
            // ✅ END BLOCK

            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === Screens ===
    public static void showLogin() {
        setRoot("/clientGUI/LoginView.fxml", "Bistro Client - Login");
    }

    public static void showCustomerMain() {
        setRoot("/clientGUI/ClientView.fxml", "Bistro Client - Main");
    }

    public static void showTerminal() {
        setRoot("/clientGUI/TerminalView.fxml", "Bistro Client - Terminal");
    }

    public static void showPayBill() {
        setRoot("/clientGUI/PayBillView.fxml", "Bistro Client - Pay Bill");
    }
    public static void showStaffDashboard() {
        setRoot("/clientGUI/StaffView.fxml", "Bistro Staff - Dashboard");
    }
}



