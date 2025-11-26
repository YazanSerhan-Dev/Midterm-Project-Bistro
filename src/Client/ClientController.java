package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ClientController {

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
        // TEMP: just show that the handler works
        statusLabel.setText("GetReservations clicked");
    }

    @FXML
    private void onUpdateReservation(ActionEvent event) {
        // TEMP: just show that the handler works
        statusLabel.setText("UpdateReservation clicked");
    }
}

