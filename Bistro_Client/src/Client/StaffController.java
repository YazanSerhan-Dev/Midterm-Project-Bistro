package Client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // ✅ Required for loading the popup
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.MakeReservationResponseDTO;
import common.dto.RegistrationDTO;
import common.dto.ReservationDTO;
import common.dto.SubscriberDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class StaffController implements ClientUI {

    @FXML private Label lblTitle;
    @FXML private Label lblStatus;

    // Tables
    @FXML private TableView<WaitingListRow> tblWaitingList;
    @FXML private TableView<ReservationRow> tblReservations;
    @FXML private TableView<SubscriberRow> tblSubscribers;
    @FXML private TableView<CurrentDinersRow> tblCurrentDiners;
    
    // Manager Views
    @FXML private VBox paneReports;

    // Manager Buttons
    @FXML private Button btnViewReports;
    @FXML private Button btnManageEmployees;

    // ✅ MISSING VARIABLE ADDED HERE
    private ReservationFormController reservationFormController;

    @FXML
    public void initialize() {
        ClientSession.bindUI(this);

        String role = ClientSession.getRole(); // "AGENT" or "MANAGER"
        String username = ClientSession.getUsername();
        
        lblTitle.setText(role + " Dashboard: " + username);

        // Hide Manager buttons if not a manager
        boolean isManager = "MANAGER".equals(role);
        if (btnViewReports != null) {
            btnViewReports.setVisible(isManager);
            btnViewReports.setManaged(isManager);
        }
        if (btnManageEmployees != null) {
            btnManageEmployees.setVisible(isManager);
            btnManageEmployees.setManaged(isManager);
        }

        // Default View
        onViewReservations(null);
    }

    // ========================================================
    // ✅ MISSING METHOD ADDED HERE (This fixes the error)
    // ========================================================
    @FXML
    private void onNewReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/clientGUI/ReservationForm.fxml"));
            Parent root = loader.load();
            
            // Capture the controller so we can pass server messages to it later
            this.reservationFormController = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("New Reservation");
            stage.setScene(new Scene(root));
            
            // Clean up reference when closed
            stage.setOnHidden(e -> this.reservationFormController = null);
            
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open reservation form: " + e.getMessage());
        }
    }

    // ========================================================
    // CLIENT UI
    // ========================================================

    @Override
    public void onConnected() {
        Platform.runLater(() -> { if (lblStatus != null) lblStatus.setText("Connected"); });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> { if (lblStatus != null) lblStatus.setText("Disconnected"); });
    }

    @Override
    public void onConnectionError(Exception e) {
        Platform.runLater(() -> { if (lblStatus != null) lblStatus.setText("Error: " + e.getMessage()); });
    }

    @Override
    public void handleServerMessage(Object msg) {
        Platform.runLater(() -> {
            Envelope env = unwrapToEnvelope(msg);
            if (env == null || !env.isOk()) return;

            switch (env.getOp()) {
                case RESPONSE_AGENT_RESERVATIONS_LIST:
                    updateReservationsTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_SUBSCRIBERS_LIST:
                    updateSubscribersTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_REGISTER_CUSTOMER:
                    showAlert("Success", "Customer registered successfully.");
                    break;

                // ✅ MISSING HANDLER LOGIC ADDED HERE
                case RESPONSE_CHECK_AVAILABILITY:
                case RESPONSE_MAKE_RESERVATION:
                    if (reservationFormController != null) {
                        MakeReservationResponseDTO dto = (MakeReservationResponseDTO) env.getPayload();
                        if (env.getOp() == OpCode.RESPONSE_CHECK_AVAILABILITY) {
                            reservationFormController.handleAvailabilityResponse(dto);
                        } else {
                            reservationFormController.handleReservationResponse(dto);
                        }
                    }
                    break;

                default:
                    System.out.println("StaffController: Unknown Op " + env.getOp());
            }
        });
    }

    // ========================================================
    // ACTIONS
    // ========================================================

    @FXML
    private void onViewReservations(ActionEvent event) {
        lblTitle.setText("Reservations");
        hideAllViews();
        setupReservationsColumns();
        tblReservations.setVisible(true); tblReservations.setManaged(true);
        sendToServer(Envelope.request(OpCode.REQUEST_AGENT_RESERVATIONS_LIST, null));
    }

    @FXML
    private void onViewSubscribers(ActionEvent event) {
        lblTitle.setText("Subscribers");
        hideAllViews();
        setupSubscriberColumns();
        tblSubscribers.setVisible(true); tblSubscribers.setManaged(true);
        sendToServer(Envelope.request(OpCode.REQUEST_SUBSCRIBERS_LIST, null));
    }

    @FXML
    private void onViewWaitingList(ActionEvent event) {
        lblTitle.setText("Waiting List");
        hideAllViews();
        showWaitingListTable(); 
    }

    @FXML
    private void onViewCurrentDiners(ActionEvent event) {
        lblTitle.setText("Current Diners");
        hideAllViews();
        showCurrentDinersTable(); 
    }

    @FXML
    private void onRegisterCustomer(ActionEvent event) {
        Dialog<RegistrationDTO> dialog = new Dialog<>();
        dialog.setTitle("Register New Customer");
        dialog.setHeaderText("Enter Customer Details");
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        
        TextField txtUsername = new TextField(); txtUsername.setPromptText("Username");
        PasswordField txtPassword = new PasswordField(); txtPassword.setPromptText("Password");
        TextField txtName = new TextField(); txtName.setPromptText("Name");
        TextField txtPhone = new TextField(); txtPhone.setPromptText("Phone");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email");
        DatePicker datePicker = new DatePicker();

        grid.add(new Label("Username:"), 0, 0); grid.add(txtUsername, 1, 0);
        grid.add(new Label("Password:"), 0, 1); grid.add(txtPassword, 1, 1);
        grid.add(new Label("Name:"), 0, 2); grid.add(txtName, 1, 2);
        grid.add(new Label("Phone:"), 0, 3); grid.add(txtPhone, 1, 3);
        grid.add(new Label("Email:"), 0, 4); grid.add(txtEmail, 1, 4);
        grid.add(new Label("Birth Date:"), 0, 5); grid.add(datePicker, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == registerButtonType) {
                String dob = (datePicker.getValue() != null) ? datePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
                return new RegistrationDTO(txtUsername.getText(), txtPassword.getText(), txtName.getText(), txtPhone.getText(), txtEmail.getText(), null, null, dob);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dto -> sendToServer(Envelope.request(OpCode.REQUEST_REGISTER_CUSTOMER, dto)));
    }

    // MANAGER ACTIONS
    @FXML
    private void onViewReports(ActionEvent event) {
        lblTitle.setText("Manager Reports");
        hideAllViews();
        paneReports.setVisible(true); paneReports.setManaged(true);
    }

    @FXML
    private void onManageEmployees(ActionEvent event) {
        lblTitle.setText("Manage Employees");
        hideAllViews();
    }

    @FXML
    private void onLogout(ActionEvent event) {
        SceneManager.showLogin();
    }

    // ========================================================
    // HELPERS
    // ========================================================

    private void hideAllViews() {
        tblReservations.setVisible(false); tblReservations.setManaged(false);
        tblSubscribers.setVisible(false); tblSubscribers.setManaged(false);
        tblWaitingList.setVisible(false); tblWaitingList.setManaged(false);
        tblCurrentDiners.setVisible(false); tblCurrentDiners.setManaged(false);
        if(paneReports != null) { paneReports.setVisible(false); paneReports.setManaged(false); }
    }

    private void updateReservationsTable(List<?> data) {
        tblReservations.getItems().clear();
        for (Object obj : data) {
            if (obj instanceof ReservationDTO dto) {
                tblReservations.getItems().add(new ReservationRow(dto.getReservationId(), dto.getConfirmationCode(), dto.getReservationTime(), dto.getExpiryTime(), dto.getNumOfCustomers(), dto.getStatus()));
            }
        }
    }

    private void updateSubscribersTable(List<?> data) {
        tblSubscribers.getItems().clear();
        for (Object obj : data) {
            if (obj instanceof SubscriberDTO dto) {
                tblSubscribers.getItems().add(new SubscriberRow(dto.getId(), dto.getFullName(), dto.getPhone(), dto.getEmail(), dto.getStatus()));
            }
        }
    }

    private void setupReservationsColumns() {
        if (!tblReservations.getColumns().isEmpty()) return;
        TableColumn<ReservationRow, Integer> colId = new TableColumn<>("ID"); colId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        TableColumn<ReservationRow, String> colCode = new TableColumn<>("Code"); colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        TableColumn<ReservationRow, String> colTime = new TableColumn<>("Time"); colTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        TableColumn<ReservationRow, Integer> colGuests = new TableColumn<>("Guests"); colGuests.setCellValueFactory(new PropertyValueFactory<>("numOfCustomers"));
        TableColumn<ReservationRow, String> colStatus = new TableColumn<>("Status"); colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tblReservations.getColumns().addAll(colId, colCode, colTime, colGuests, colStatus);
    }

    private void setupSubscriberColumns() {
        if (!tblSubscribers.getColumns().isEmpty()) return;
        TableColumn<SubscriberRow, Integer> colId = new TableColumn<>("ID"); colId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        TableColumn<SubscriberRow, String> colName = new TableColumn<>("Name"); colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        TableColumn<SubscriberRow, String> colPhone = new TableColumn<>("Phone"); colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        TableColumn<SubscriberRow, String> colEmail = new TableColumn<>("Email"); colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        tblSubscribers.getColumns().addAll(colId, colName, colPhone, colEmail);
    }
    
    private void showWaitingListTable() {
        tblWaitingList.setVisible(true); tblWaitingList.setManaged(true);
        if(tblWaitingList.getColumns().isEmpty()) {
             TableColumn<WaitingListRow, String> colName = new TableColumn<>("Name"); colName.setCellValueFactory(new PropertyValueFactory<>("name"));
             tblWaitingList.getColumns().add(colName);
        }
        tblWaitingList.setItems(FXCollections.observableArrayList(new WaitingListRow(1, "Ahmad", "050-123", 4, "Waiting", "A")));
    }
    
    private void showCurrentDinersTable() {
        tblCurrentDiners.setVisible(true); tblCurrentDiners.setManaged(true);
        if(tblCurrentDiners.getColumns().isEmpty()) {
             TableColumn<CurrentDinersRow, Integer> colTable = new TableColumn<>("Table"); colTable.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
             tblCurrentDiners.getColumns().add(colTable);
        }
        tblCurrentDiners.setItems(FXCollections.observableArrayList(new CurrentDinersRow(5, "Sara", 4, "18:00", "Dining")));
    }

    private void sendToServer(Envelope env) {
        try {
            BistroClient client = ClientSession.getClient();
            if (client != null && client.isConnected()) {
                client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }
    
    private Envelope unwrapToEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope e) return e;
            if (msg instanceof KryoMessage km) return (Envelope) KryoUtil.fromBytes(km.getPayload());
        } catch (Exception e) {}
        return null;
    }
}