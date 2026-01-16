package clientGUI;

import javafx.application.Application;

import javafx.stage.Stage;

import Client.ClientController;
import Client.ClientSession;
import Client.SceneManager;

/**
 * Entry point of the Bistro Client application.
 *
 * This class is responsible for:
 * <ul>
 *   <li>Initializing the JavaFX application</li>
 *   <li>Configuring server connection parameters (host and port)</li>
 *   <li>Initializing the {@link SceneManager}</li>
 *   <li>Launching the first screen (Login)</li>
 * </ul>
 *
 * The application can optionally receive command-line arguments
 * for server host and port:
 * <pre>
 * java -jar client.jar &lt;host&gt; &lt;port&gt;
 * </pre>
 */
public class ClientFX extends Application {

    private ClientController mainController;
    /**
     * JavaFX application start method.
     *
     * Initializes the primary stage, configures the client session
     * with server connection details, and displays the login screen.
     * Also ensures a clean disconnection from the server when the
     * application window is closed.
     *
     * @param primaryStage the main application window
     */
    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);

        // Default connection settings
        String host = "localhost";
        int port = 5555;

        // read args: java -jar client.jar 192.168.1.42 5555
        var args = getParameters().getRaw();
        if (args.size() >= 1) host = args.get(0);
        if (args.size() >= 2) port = safeParsePort(args.get(1), 5555);
        // Configure shared client session
        ClientSession.configure(host, port);

        SceneManager.showLogin();
        primaryStage.setOnCloseRequest(e -> ClientSession.disconnect());
    }


    /**
     * Safely parses a port number from a string.
     *
     * If parsing fails or the value is out of the valid port range
     * (1–65535), a default value is returned.
     *
     * @param s   the string containing the port number
     * @param def the default port to use if parsing fails
     * @return a valid port number
     */
    private int safeParsePort(String s, int def) {
        try {
            int p = Integer.parseInt(s.trim());
            return (p >= 1 && p <= 65535) ? p : def;
        } catch (Exception e) {
            return def;
        }
    }
    /**
     * Main method that launches the JavaFX application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        launch(args);
    }
}









