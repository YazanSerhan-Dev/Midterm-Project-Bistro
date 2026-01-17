package Client;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * Central navigation utility for switching between JavaFX scenes (FXML screens).
 * <p>
 * This class loads FXML files, sets them as the active scene on the primary {@link Stage},
 * and keeps a simple navigation history to support "Back" behavior.
 * </p>
 * <p>
 * If the loaded controller implements {@link ClientUI}, it is automatically bound to the
 * shared client connection via {@link ClientSession#bindUI(ClientUI)} so the current screen
 * can receive server callbacks.
 * </p>
 */
public class SceneManager {

    private static Stage stage;
    private static final Deque<String> history = new ArrayDeque<>();
    private static String currentFxml = null;
    private static String currentTitle = null;
    /**
     * Initializes the SceneManager with the primary application stage.
     * Must be called once during application startup (e.g., from {@code start()}).
     *
     * @param primaryStage the primary JavaFX stage used for displaying scenes
     */
    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    /**
     * Loads an FXML file and sets it as the root of the current stage scene.
     * Also binds the controller to the shared client if it implements {@link ClientUI}.
     *
     * @param fxmlPath path to the FXML resource (classpath resource)
     * @param title    window title to display
     */
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
    /**
     * Navigates to the login screen and resets navigation history.
     */
    public static void showLogin() {
        history.clear();
        currentFxml = null;
        currentTitle = null;
        setRoot("/clientGUI/LoginView.fxml", "Bistro Client - Login");
    }
    /**
     * Navigates to the main customer screen (client view) and stores history.
     */
    public static void showCustomerMain() {
        setRootWithHistory("/clientGUI/ClientView.fxml", "Bistro Client - Main");
    }
    /**
     * Navigates to the terminal screen and stores history.
     */
    public static void showTerminal(boolean subscriberMode) {
        setRootWithHistoryTerminalMode("/clientGUI/TerminalView.fxml",
                "Bistro Client - Terminal",
                subscriberMode);
    }
    
    public static void showTerminalEntry() {
        setRootWithHistory("/clientGUI/TerminalEntryView.fxml",
                "Bistro Client - Terminal");
    }
    /**
     * Navigates to the pay bill screen and stores history.
     */
    public static void showPayBill() {
        setRootWithHistory("/clientGUI/PayBillView.fxml", "Bistro Client - Pay Bill");
    }
    /**
     * Navigates to the staff dashboard screen and stores history.
     */
    public static void showStaffDashboard() {
        setRootWithHistory("/clientGUI/StaffView.fxml", "Bistro Staff - Dashboard");
    }
    /**
     * Navigates to the given FXML screen while recording the previous screen in history,
     * allowing {@link #goBack()} to return to it later.
     *
     * @param fxmlPath path to the FXML resource
     * @param title    window title for the target screen
     */
    private static void setRootWithHistory(String fxmlPath, String title) {
        if (currentFxml != null && !currentFxml.equals(fxmlPath)) {
            // store BOTH fxml and title in one string
            history.push(currentFxml + "||" + currentTitle);
        }
        currentFxml = fxmlPath;
        currentTitle = title;

        setRoot(fxmlPath, title); // <-- your existing method
    }
    /**
     * Navigates back to the previous screen in history.
     * If no history exists, falls back to the customer main screen.
     */
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
        
        if ("/clientGUI/TerminalView.fxml".equals(fxml)) {
            boolean sub = "SUBSCRIBER".equalsIgnoreCase(ClientSession.getRole())
                    && ClientSession.getUsername() != null
                    && !ClientSession.getUsername().isBlank();

            showTerminal(sub);
            return;
        }
        setRoot(fxml, title);
    }
    
    private static void setRootWithHistoryTerminalMode(String fxmlPath, String title, boolean subscriberMode) {
        if (currentFxml != null && !currentFxml.equals(fxmlPath)) {
            history.push(currentFxml + "||" + currentTitle);
        }
        currentFxml = fxmlPath;
        currentTitle = title;

        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();

            // ✅ Bind UI like you already do
            if (controller instanceof ClientUI ui) {
                ClientSession.bindUI(ui);
            }

            // ✅ Pass mode ONLY if this is the terminal controller
            if (controller instanceof TerminalController tc) {
                tc.setSubscriberModeIntent(subscriberMode);
            }

            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}



