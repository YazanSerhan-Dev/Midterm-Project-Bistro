package Client;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.MakeReservationRequestDTO;
import common.dto.MakeReservationResponseDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
/**
 * JavaFX controller for the reservation creation form (popup/window).
 * <p>
 * Allows a user to select a reservation date/time and number of diners,
 * check availability, and confirm a reservation request.
 * </p>
 * <p>
 * Requests are sent to the server using the shared {@link BistroClient} instance
 * provided by {@link ClientSession}. Responses are applied to the UI via
 * {@link #handleAvailabilityResponse(MakeReservationResponseDTO)} and
 * {@link #handleReservationResponse(MakeReservationResponseDTO)}.
 * </p>
 */
public class ReservationFormController {

    @FXML private TextField tfDiners;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbTime;

    // Customer Info Inputs
    @FXML private RadioButton rbSubscriber;
    @FXML private RadioButton rbCasual;
    @FXML private TextField tfSubscriberID;
    @FXML private TextField tfPhone;
    @FXML private TextField tfEmail;
    
    @FXML private Button btnConfirm;
    @FXML private Button btnCheck;
    @FXML private Label lblStatus;
    @FXML private ListView<String> listAlternatives;

    private boolean availabilityConfirmed = false;

    /**
     * Initializes the reservation form after the FXML has been loaded.
     * <p>
     * Populates the time list with half-hour intervals, sets default selections,
     * and configures the UI toggle logic for subscriber vs. casual/guest reservation.
     * </p>
     */
    @FXML
    public void initialize() {
        // Init time slots (30 min intervals)
        List<String> times = new ArrayList<>();
        for (int h = 10; h < 24; h++) { // Reasonable restaurant hours
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        cbTime.setItems(FXCollections.observableArrayList(times));
        cbTime.getSelectionModel().select("18:00");
        
        if (dpDate.getValue() == null) {
            dpDate.setValue(LocalDate.now());
        }

        // Toggle logic
        ToggleGroup group = new ToggleGroup();
        rbSubscriber.setToggleGroup(group);
        rbCasual.setToggleGroup(group);
        rbCasual.setSelected(true); // Default to Casual/Guest
        toggleInputs();

        rbSubscriber.setOnAction(e -> toggleInputs());
        rbCasual.setOnAction(e -> toggleInputs());
    }

    /**
     * Updates input fields based on the selected reservation type.
     * Subscriber reservations use a subscriber ID, while casual/guest reservations use
     * phone and email fields.
     */
    private void toggleInputs() {
        boolean isSub = rbSubscriber.isSelected();
        tfSubscriberID.setDisable(!isSub);
        tfPhone.setDisable(isSub);
        tfEmail.setDisable(isSub);
    }
    /**
     * UI action: checks reservation availability for the selected date, time, and number of diners.
     * Sends an availability request to the server and clears any previously suggested alternatives.
     *
     * @param event action event from the button click
     */
    @FXML
    private void onCheckAvailability(ActionEvent event) {
        lblStatus.setText("Checking...");
        listAlternatives.getItems().clear();
        btnConfirm.setDisable(true);
        availabilityConfirmed = false;

        try {
            // 1. Validation
            int diners = Integer.parseInt(tfDiners.getText());
            if (dpDate.getValue() == null || cbTime.getValue() == null) {
                lblStatus.setText("Select date & time.");
                return;
            }
            LocalDateTime reqDateTime = LocalDateTime.of(dpDate.getValue(), LocalTime.parse(cbTime.getValue()));

            // 2. Create Request DTO
            MakeReservationRequestDTO req = new MakeReservationRequestDTO(
                null, null, null, diners, Timestamp.valueOf(reqDateTime)
            );

            // 3. Send using ClientSession (The correct way)
            sendRequest(OpCode.REQUEST_CHECK_AVAILABILITY, req);

        } catch (NumberFormatException e) {
            lblStatus.setText("Invalid number of diners.");
        }
    }

    /**
     * UI action: confirms the reservation request after availability has been confirmed.
     * Sends a reservation request to the server using subscriber or guest details based on the selected mode.
     *
     * @param event action event from the button click
     */
    @FXML
    private void onConfirm(ActionEvent event) {
        if (!availabilityConfirmed) return;

        try {
            int diners = Integer.parseInt(tfDiners.getText());
            LocalDateTime reqDateTime = LocalDateTime.of(dpDate.getValue(), LocalTime.parse(cbTime.getValue()));
            Timestamp ts = Timestamp.valueOf(reqDateTime);

            String subID = rbSubscriber.isSelected() ? tfSubscriberID.getText() : null;
            String phone = rbCasual.isSelected() ? tfPhone.getText() : null;
            String email = rbCasual.isSelected() ? tfEmail.getText() : null;

            MakeReservationRequestDTO req = new MakeReservationRequestDTO(
                subID, phone, email, diners, ts
            );

            sendRequest(OpCode.REQUEST_MAKE_RESERVATION, req);
            lblStatus.setText("Sending reservation...");

        } catch (Exception e) {
            lblStatus.setText("Error sending request.");
        }
    }
    /**
     * Sends an envelope-based request to the server using the shared client connection.
     *
     * @param op operation code indicating the request type
     * @param payload request payload object (DTO or other supported object)
     */
    // HELPER: Sends message correctly using ClientSession
    private void sendRequest(OpCode op, Object payload) {
        BistroClient client = ClientSession.getClient();
        if (client == null || !client.isConnected()) {
            lblStatus.setText("Error: Not connected to server.");
            return;
        }

        try {
            // Wrap in Envelope -> Byte Array -> KryoMessage
            Envelope env = Envelope.request(op, payload);
            byte[] data = KryoUtil.toBytes(env);
            client.sendToServer(new KryoMessage("ENVELOPE", data));
        } catch (Exception e) {
            lblStatus.setText("Send failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Applies the server response for availability check.
     * <p>
     * If available, enables the confirm button. Otherwise, shows the server message
     * and displays suggested alternative times (if provided).
     * </p>
     *
     * @param res server response DTO for availability check
     */
    // ... Response Handlers (called by StaffController) ...
    public void handleAvailabilityResponse(MakeReservationResponseDTO res) {
         Platform.runLater(() -> {
            if (res.isOk()) {
                lblStatus.setText("Available! Click Confirm.");
                lblStatus.setStyle("-fx-text-fill: green;");
                btnConfirm.setDisable(false);
                availabilityConfirmed = true;
            } else {
                lblStatus.setText(res.getMessage());
                lblStatus.setStyle("-fx-text-fill: red;");
                if (res.getSuggestedTimes() != null) {
                    for (Timestamp t : res.getSuggestedTimes()) {
                        listAlternatives.getItems().add(t.toLocalDateTime().toString());
                    }
                }
            }
        });
    }
    /**
     * Applies the server response for creating a reservation.
     * <p>
     * On success, displays the confirmation code and closes the form window.
     * On failure, shows the error message on the form.
     * </p>
     *
     * @param res server response DTO for reservation creation
     */
    public void handleReservationResponse(MakeReservationResponseDTO res) {
        Platform.runLater(() -> {
            if (res.isOk()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Reservation Confirmed");
                alert.setContentText("Code: " + res.getConfirmationCode());
                alert.showAndWait();
                
                // Close the popup
                ((Stage) btnConfirm.getScene().getWindow()).close();
            } else {
                lblStatus.setText("Error: " + res.getMessage());
                lblStatus.setStyle("-fx-text-fill: red;");
            }
        });
    }
}