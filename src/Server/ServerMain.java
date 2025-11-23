import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ServerMain {

    public static void main(String[] args) {
        System.out.println("Server started...");

        try (ServerSocket serverSocket = new ServerSocket(5555)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

                BufferedReader input = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                PrintWriter output = new PrintWriter(
                        clientSocket.getOutputStream(), true);

                String message = input.readLine();
                System.out.println("Received: " + message);

                // For now just send a simple reply
                output.println("Server received: " + message);

                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
