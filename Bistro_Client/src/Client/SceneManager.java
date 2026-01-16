package Client;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage stage;
    private static final Deque<String> history = new ArrayDeque<>();
    private static String currentFxml = null;
    private static String currentTitle = null;

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
        history.clear();
        currentFxml = null;
        currentTitle = null;
        setRoot("/clientGUI/LoginView.fxml", "Bistro Client - Login");
    }

    public static void showCustomerMain() {
        setRootWithHistory("/clientGUI/ClientView.fxml", "Bistro Client - Main");
    }

    public static void showTerminal() {
        setRootWithHistory("/clientGUI/TerminalView.fxml", "Bistro Client - Terminal");
    }

    public static void showPayBill() {
        setRootWithHistory("/clientGUI/PayBillView.fxml", "Bistro Client - Pay Bill");
    }

    public static void showStaffDashboard() {
        setRootWithHistory("/clientGUI/StaffView.fxml", "Bistro Staff - Dashboard");
    }
    
    private static void setRootWithHistory(String fxmlPath, String title) {
        if (currentFxml != null && !currentFxml.equals(fxmlPath)) {
            // store BOTH fxml and title in one string
            history.push(currentFxml + "||" + currentTitle);
        }
        currentFxml = fxmlPath;
        currentTitle = title;

        setRoot(fxmlPath, title); // <-- your existing method
    }

    public static void goBack() {
        if (history.isEmpty()) {
            // fallback
            showCustomerMain();
            return;
        }

        String prev = history.pop();
        String[] parts = prev.split("\\|\\|", 2);

        String fxml = parts[0];
        String title = (parts.length > 1) ? parts[1] : "Bistro";

        currentFxml = fxml;
        currentTitle = title;

        setRoot(fxml, title);
    }


}



