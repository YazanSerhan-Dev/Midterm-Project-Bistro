package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientMain {

	public static void main(String[] args) {
	    String serverHost = "localhost";
	    int serverPort = 5555;

	    try (Socket socket = new Socket(serverHost, serverPort)) {
	        System.out.println("Connected to server");

	        PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
	        BufferedReader input = new BufferedReader(
	                new InputStreamReader(socket.getInputStream()));

	        // send correct command
	        output.println("GET_RESERVATIONS");

	        // read all server output
	        String response;
	        while ((response = input.readLine()) != null) {
	            System.out.println(response);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
