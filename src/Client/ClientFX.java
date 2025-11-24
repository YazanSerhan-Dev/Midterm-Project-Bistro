package Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientFX extends Application {

    private TextArea reservationsArea;
    private TextField reservationNumberField;
    private TextField dateField;
    private TextField guestsField;
    private Label statusLabel;

    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 5555;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Bistro Client - Reservations");

        // ---- Top area: reservations list ----
        reservationsArea = new TextArea();
        reservationsArea.setEditable(false);
        reservationsArea.setPrefRowCount(10);

        // ---- Middle: update form ----
        reservationNumberField = new TextField();
        reservationNumberField.setPromptText("Reservation number (e.g., 1)");

        dateField = new TextField();
        dateField.setPromptText("New date (yyyy-mm-dd)");

        guestsField = new TextField();
        guestsField.setPromptText("Number of guests");

        HBox formBox = new HBox(10,
                new Label("Reservation #:"), reservationNumberField,
                new Label("Date:"), dateField,
                new Label("Guests:"), guestsField
        );
        formBox.setPadding(new Insets(10));

        // ---- Buttons ----
        Button getBtn = new Button("Get Reservations");
        Button updateBtn = new Button("Update Reservation");

        getBtn.setOnAction(e -> getReservations());
        updateBtn.setOnAction(e -> updateReservation());

        HBox buttonsBox = new HBox(10, getBtn, updateBtn);
        buttonsBox.setPadding(new Insets(10));

        // ---- Status label ----
        statusLabel = new Label("Status: idle");
        HBox statusBox = new HBox(statusLabel);
        statusBox.setPadding(new Insets(10));

        // ---- Layout ----
        VBox root = new VBox(10,
                new Label("Reservations from server:"),
                reservationsArea,
                formBox,
                buttonsBox,
                statusBox
        );
        root.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(root, 800, 400));
        primaryStage.show();
    }

    // ============== COMMUNICATION METHODS =================

    private void getReservations() {
        statusLabel.setText("Status: requesting reservations...");
        reservationsArea.clear();

        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))) {

                out.println("GET_RESERVATIONS");

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line).append("\n");
                }

                String result = sb.length() == 0 ? "NO_RESERVATIONS" : sb.toString();

                Platform.runLater(() -> {
                    reservationsArea.setText(result);
                    statusLabel.setText("Status: reservations loaded");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        statusLabel.setText("Status: ERROR - " + ex.getMessage()));
            }
        }).start();
    }

    private void updateReservation() {
        String numberText = reservationNumberField.getText().trim();
        String dateText = dateField.getText().trim();
        String guestsText = guestsField.getText().trim();

        if (numberText.isEmpty() || dateText.isEmpty() || guestsText.isEmpty()) {
            statusLabel.setText("Status: please fill all fields");
            return;
        }

        // very simple validation
        int reservationNumber;
        int guests;
        try {
            reservationNumber = Integer.parseInt(numberText);
            guests = Integer.parseInt(guestsText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Status: reservation # and guests must be numbers");
            return;
        }

        String command = "UPDATE_RESERVATION:" +
                reservationNumber + ":" + dateText + ":" + guests;

        statusLabel.setText("Status: sending update...");

        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))) {

                out.println(command);
                String response = in.readLine();

                Platform.runLater(() ->
                        statusLabel.setText("Status: server replied - " + response));

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        statusLabel.setText("Status: ERROR - " + ex.getMessage()));
            }
        }).start();
    }
}
