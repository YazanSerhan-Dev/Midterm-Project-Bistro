package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ClientController {
	
	private static final String SERVER_HOST = "192.168.33.3";
	private static final int SERVER_PORT = 5555;


    @FXML private TextArea reservationsArea;
    @FXML private TextField reservationNumberField;
    @FXML private TextField dateField;
    @FXML private TextField guestsField;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("Status: idle");
        }
    }

    @FXML
    private void onGetReservations(ActionEvent event) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // send command to server
            out.println("GET_RESERVATIONS");

            // read reply from server
            String result = in.readLine();
            reservationsArea.setText(result);

            statusLabel.setText("reservations loaded");

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onUpdateReservation(ActionEvent event) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // collect user input
            int num = Integer.parseInt(reservationNumberField.getText());
            String date = dateField.getText();
            int guests = Integer.parseInt(guestsField.getText());

            // build update command
            String cmd = "UPDATE_RESERVATION:" + num + ":" + date + ":" + guests;

            // send to server
            out.println(cmd);

            // read reply
            String reply = in.readLine();
            statusLabel.setText(reply);

        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

