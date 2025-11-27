package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain {

    public static void main(String[] args) {
        String serverHost = "192.168.33.3";
        int serverPort = 5555;

        try (Socket socket = new Socket(serverHost, serverPort)) {
            System.out.println("Connected to server");

            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // 🔹 1) send update command:
            // here we update reservation number 1
            // to date 2025-02-01 with 6 guests
            String command = "UPDATE_RESERVATION:1:2025-02-01:6";
            output.println(command);
            System.out.println("Sent: " + command);

            // 🔹 2) read server reply
            String response = input.readLine();
            System.out.println("Server replied: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

