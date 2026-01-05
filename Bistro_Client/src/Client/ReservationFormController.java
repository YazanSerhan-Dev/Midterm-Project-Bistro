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

    private void toggleInputs() {
        boolean isSub = rbSubscriber.isSelected();
        tfSubscriberID.setDisable(!isSub);
        tfPhone.setDisable(isSub);
        tfEmail.setDisable(isSub);
    }

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

    // ✅ HELPER: Sends message correctly using ClientSession
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