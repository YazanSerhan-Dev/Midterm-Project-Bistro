package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain {

    public static void main(String[] args) {
        String serverHost = "localhost"; // later we will show this in the GUI
        int serverPort = 5555;

        try (Socket socket = new Socket(serverHost, serverPort)) {
            System.out.println("Connected to server");

            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // For now send a simple test message
            output.println("Hello from client!");

            String response = input.readLine();
            System.out.println("Server replied: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
