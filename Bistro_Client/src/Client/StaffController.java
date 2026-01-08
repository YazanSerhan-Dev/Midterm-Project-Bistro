package Client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import common.dto.MakeReservationRequestDTO;
import common.dto.MakeReservationResponseDTO;
import common.dto.RegistrationDTO;
import common.dto.ReservationDTO;
import common.dto.SubscriberDTO;
import common.dto.WaitingListDTO;
// ✅ Add these imports
import common.dto.LoginRequestDTO;
import common.dto.LoginResponseDTO;

import java.io.IOException;
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
    @FXML private VBox paneWaitingList;
    @FXML private Button btnViewReports;
    @FXML private Button btnManageEmployees;
    
    // ✅ REMOVED: private ReservationFormController reservationFormController; 

    @FXML
    public void initialize() {
        ClientSession.bindUI(this);
        String role = ClientSession.getRole();
        String username = ClientSession.getUsername();
        lblTitle.setText(role + " Dashboard: " + username);

        boolean isManager = "MANAGER".equals(role);
        if (btnViewReports != null) {
            btnViewReports.setVisible(isManager);
            btnViewReports.setManaged(isManager);
        }
        if (btnManageEmployees != null) {
            btnManageEmployees.setVisible(isManager);
            btnManageEmployees.setManaged(isManager);
        }
        onViewReservations(null);
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
                case RESPONSE_WAITING_LIST:
                    updateWaitingListTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_CURRENT_DINERS:
                    updateCurrentDinersTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_WAITING_ADD:
                case RESPONSE_WAITING_ASSIGN:
                case RESPONSE_WAITING_REMOVE:
                    if (env.getPayload() instanceof String s) showAlert("Success", s);
                    sendToServer(new Envelope(OpCode.REQUEST_WAITING_LIST));
                    break;
                
                // ✅ ADDED: Handle subscriber login response for redirection
                case RESPONSE_LOGIN_SUBSCRIBER:
                    if (env.getPayload() instanceof LoginResponseDTO res) {
                        if (res.isOk()) {
                            ClientSession.setRole("SUBSCRIBER");
                            ClientSession.setUsername(res.getUsername());
                            SceneManager.showCustomerMain();
                        } else {
                            showAlert("Validation Failed", res.getMessage());
                        }
                    }
                    break;

                // ✅ REMOVED: RESPONSE_CHECK_AVAILABILITY and RESPONSE_MAKE_RESERVATION cases

                default:
                    System.out.println("StaffController: Unknown Op " + env.getOp());
            }
        });
    }

    // ========================================================
    // ACTIONS
    // ========================================================

    // ✅ ADDED: "Act as Customer" logic replacing onNewReservation
    @FXML
    public void onActAsCustomer(ActionEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Act as Customer");
        dialog.setHeaderText("Impersonate Customer/Subscriber");

        ButtonType connectBtn = new ButtonType("Start Session", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        RadioButton rbSub = new RadioButton("Subscriber");
        RadioButton rbGuest = new RadioButton("Guest (Walk-in)");
        ToggleGroup tg = new ToggleGroup();
        rbSub.setToggleGroup(tg); rbGuest.setToggleGroup(tg);
        rbSub.setSelected(true);

        TextField tfUsername = new TextField(); tfUsername.setPromptText("Username");
        PasswordField pfPassword = new PasswordField(); pfPassword.setPromptText("Password");
        TextField tfEmail = new TextField(); tfEmail.setPromptText("Guest Email");
        TextField tfPhone = new TextField(); tfPhone.setPromptText("Guest Phone");

        grid.add(rbSub, 0, 0); grid.add(rbGuest, 1, 0);
        grid.add(new Label("Username:"), 0, 1); grid.add(tfUsername, 1, 1);
        grid.add(new Label("Password:"), 0, 2); grid.add(pfPassword, 1, 2);
        grid.add(new Label("Email:"), 0, 3);    grid.add(tfEmail, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);    grid.add(tfPhone, 1, 4);

        Runnable updateState = () -> {
            boolean isSub = rbSub.isSelected();
            tfUsername.setDisable(!isSub); pfPassword.setDisable(!isSub);
            tfEmail.setDisable(isSub); tfPhone.setDisable(isSub);
        };
        rbSub.setOnAction(e -> updateState.run());
        rbGuest.setOnAction(e -> updateState.run());
        updateState.run();

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == connectBtn) {
                if (rbSub.isSelected()) return "SUB:" + tfUsername.getText() + ":" + pfPassword.getText();
                else return "GUEST:" + tfEmail.getText() + ":" + tfPhone.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String[] parts = result.split(":", 3);
            if ("SUB".equals(parts[0])) {
                String user = parts.length > 1 ? parts[1] : "";
                String pass = parts.length > 2 ? parts[2] : "";
                sendToServer(Envelope.request(OpCode.REQUEST_LOGIN_SUBSCRIBER, new LoginRequestDTO(user, pass)));
            } else {
                ClientSession.setRole("CUSTOMER");
                ClientSession.setUsername("Guest");
                ClientSession.setGuestEmail(parts.length > 1 ? parts[1] : "");
                ClientSession.setGuestPhone(parts.length > 2 ? parts[2] : "");
                SceneManager.showCustomerMain();
            }
        });
    }

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
        setupWaitingListColumns();
        
        // ✅ FIX: Show the wrapper pane, not just the table
        if (paneWaitingList != null) {
            paneWaitingList.setVisible(true);
            paneWaitingList.setManaged(true);
        }
        
        // Also ensure table is visible inside it
        tblWaitingList.setVisible(true); 
        tblWaitingList.setManaged(true);
        
        sendToServer(Envelope.request(OpCode.REQUEST_WAITING_LIST, null));
    }
    @FXML
    private void onViewCurrentDiners(ActionEvent event) {
        lblTitle.setText("Current Diners");
        hideAllViews();
        
        // 1. Setup the columns
        setupCurrentDinersColumns();
        
        // 2. Make the table visible
        if (tblCurrentDiners != null) {
            tblCurrentDiners.setVisible(true);
            tblCurrentDiners.setManaged(true);
        }

        // 3. IMPORTANT: Send the request to the server!
        // If this line is missing, the server will never know you clicked the button.
        sendToServer(Envelope.request(OpCode.REQUEST_CURRENT_DINERS, null)); 
    }

    @FXML
    private void onRegisterCustomer(ActionEvent event) {
        // ... (Keep existing implementation) ...
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
        
        // ✅ FIX: Hide the wrapper pane
        if (paneWaitingList != null) {
            paneWaitingList.setVisible(false);
            paneWaitingList.setManaged(false);
        } else if (tblWaitingList.getParent() != null) {
             // Fallback if FXML isn't updated yet, though fx:id is preferred
             tblWaitingList.getParent().setVisible(false);
             tblWaitingList.getParent().setManaged(false);
        }

        tblCurrentDiners.setVisible(false); tblCurrentDiners.setManaged(false);
        if(paneReports != null) { paneReports.setVisible(false); paneReports.setManaged(false); }
    }

    // ... (Keep updateReservationsTable, updateSubscribersTable, setup columns etc.) ...
    private void updateReservationsTable(List<?> data) {
        tblReservations.getItems().clear();
        for (Object obj : data) {
            if (obj instanceof ReservationDTO dto) {
                tblReservations.getItems().add(new ReservationRow(dto.getReservationId(), dto.getConfirmationCode(), dto.getReservationTime(), dto.getExpiryTime(), dto.getNumOfCustomers(), dto.getStatus()));
            }
        }
    }

    private void updateSubscribersTable(List<?> data) {
        tblSubscribers.getItems().clear(); // Fixed typo: was clearing tblWaitingList
        for (Object obj : data) {
            if (obj instanceof SubscriberDTO dto) {
                tblSubscribers.getItems().add(new SubscriberRow(dto.getId(), dto.getFullName(), dto.getPhone(), dto.getEmail(), dto.getStatus()));
            }
        }
    }
    
    private void updateWaitingListTable(List<?> data) {
        tblWaitingList.getItems().clear();
        for (Object obj : data) {
            if (obj instanceof WaitingListDTO dto) {
                WaitingListRow row = new WaitingListRow(dto.getWaitingId(),dto.getNumOfCustomers(),dto.getRequestTime(),dto.getStatus(),dto.getConfirmationCode());
                tblWaitingList.getItems().add(row);
            }
        }
    }
    private void updateCurrentDinersTable(List<?> data) {
        tblCurrentDiners.getItems().clear();
        for (Object obj : data) {
            if (obj instanceof common.dto.CurrentDinersDTO dto) {
                tblCurrentDiners.getItems().add(new CurrentDinersRow(
                    dto.getTableNumber(),
                    dto.getCustomerName(),
                    dto.getPeopleCount(),
                    dto.getCheckInTime(),
                    dto.getStatus()
                ));
            }
        }
    }

    private void setupWaitingListColumns() {
        if (!tblWaitingList.getColumns().isEmpty()) return;
        TableColumn<WaitingListRow, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("waitingId"));
        TableColumn<WaitingListRow, Integer> colCount = new TableColumn<>("Customers");
        colCount.setCellValueFactory(new PropertyValueFactory<>("numOfCustomers"));
        TableColumn<WaitingListRow, String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(new PropertyValueFactory<>("requestTime"));
        TableColumn<WaitingListRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<WaitingListRow, String> colCode = new TableColumn<>("Code"); 
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        tblWaitingList.getColumns().addAll(colId, colCount, colTime, colStatus, colCode);
    }
    
    private void setupReservationsColumns() {
        if (!tblReservations.getColumns().isEmpty()) return;
        TableColumn<ReservationRow, Integer> colId = new TableColumn<>("ID"); 
        colId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));
        TableColumn<ReservationRow, String> colCode = new TableColumn<>("Code"); 
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        TableColumn<ReservationRow, String> colTime = new TableColumn<>("Time"); 
        colTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        TableColumn<ReservationRow, Integer> colGuests = new TableColumn<>("Guests"); 
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numOfCustomers"));
        TableColumn<ReservationRow, String> colStatus = new TableColumn<>("Status"); 
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tblReservations.getColumns().addAll(colId, colCode, colTime, colGuests, colStatus);
    }

    private void setupSubscriberColumns() {
        if (!tblSubscribers.getColumns().isEmpty()) return;
        TableColumn<SubscriberRow, Integer> colId = new TableColumn<>("ID"); 
        colId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        TableColumn<SubscriberRow, String> colName = new TableColumn<>("Name"); 
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        TableColumn<SubscriberRow, String> colPhone = new TableColumn<>("Phone"); 
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        TableColumn<SubscriberRow, String> colEmail = new TableColumn<>("Email"); 
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        tblSubscribers.getColumns().addAll(colId, colName, colPhone, colEmail);
    }
    
    private void setupCurrentDinersColumns() {
        if (!tblCurrentDiners.getColumns().isEmpty()) return;

        TableColumn<CurrentDinersRow, Number> colTable = new TableColumn<>("Table");
        colTable.setCellValueFactory(cell -> cell.getValue().tableNumberProperty());

        TableColumn<CurrentDinersRow, String> colName = new TableColumn<>("Customer Name");
        colName.setCellValueFactory(cell -> cell.getValue().customerNameProperty());
        colName.setPrefWidth(150);

        TableColumn<CurrentDinersRow, Number> colCount = new TableColumn<>("People");
        colCount.setCellValueFactory(cell -> cell.getValue().peopleCountProperty());

        TableColumn<CurrentDinersRow, String> colTime = new TableColumn<>("Check-in Time");
        colTime.setCellValueFactory(cell -> cell.getValue().checkInTimeProperty());
        colTime.setPrefWidth(120);

        TableColumn<CurrentDinersRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());

        tblCurrentDiners.getColumns().addAll(colTable, colName, colCount, colTime, colStatus);
    }

    // ✅ FIXED: Renamed to match FXML (was addWaitingCustomer)
    @FXML
    void onAddWaitingCustomer(ActionEvent event) {
        Dialog<MakeReservationRequestDTO> dialog = new Dialog<>();
        dialog.setTitle("Add Walk-in Customer");
        dialog.setHeaderText("Enter Walk-in Details");
        
        ButtonType loginButtonType= new ButtonType("Add to Waiting List", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        
        TextField tfName = new TextField();
        TextField tfSize = new TextField();
        TextField tfPhone = new TextField();
        CheckBox IsSubscriber = new CheckBox();
        TextField tfSubscriberId = new TextField();
        tfSubscriberId.setPromptText("Subscriber Id");
        tfSubscriberId.setDisable(true);
        
        IsSubscriber.setOnAction(e -> {
            tfSubscriberId.setDisable(!IsSubscriber.isSelected());
            tfName.setDisable(IsSubscriber.isSelected());
            tfPhone.setDisable(IsSubscriber.isSelected());
        });
        
        grid.add(new Label("Group Size:"), 0, 0); grid.add(tfSize, 1, 0);
        grid.add(IsSubscriber, 0, 1);             grid.add(tfSubscriberId, 1, 1);
        grid.add(new Label("Name/Phone (Guest):"), 0, 2); grid.add(tfPhone, 1, 2);

        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                try {
                    int size = Integer.parseInt(tfSize.getText());
                    String subId = IsSubscriber.isSelected() ? tfSubscriberId.getText() : null;
                    String phone = !IsSubscriber.isSelected() ? tfPhone.getText() : null;
                    return new MakeReservationRequestDTO(subId, phone, null, size, null); 
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dto -> {
        	sendToServer(Envelope.request(OpCode.REQUEST_WAITING_ADD, dto));
        	});
    }
    
    // ✅ FIXED: Uncommented methods so the buttons in FXML work
    @FXML
    void onAssignTable(ActionEvent event) {
        WaitingListRow selected = tblWaitingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select a customer from the waiting list first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Assign Table");
        dialog.setHeaderText("Assigning Customer ID: " + selected.getWaitingId());
        dialog.setContentText("Enter Table Number (e.g. T01):");

        dialog.showAndWait().ifPresent(tableId -> {
            Object[] payload = new Object[]{ selected.getWaitingId(), tableId };
            sendToServer(Envelope.request(OpCode.REQUEST_WAITING_ASSIGN, payload));
        });
    }

    @FXML
    void onRemoveWaitingCustomer(ActionEvent event) {
        WaitingListRow selected = tblWaitingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select a customer to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Remove request ID " + selected.getWaitingId() + "?", ButtonType.YES, ButtonType.NO);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
            	sendToServer(Envelope.request(OpCode.REQUEST_WAITING_REMOVE, selected.getWaitingId()));
            }
        });
    }

    private void sendToServer(Envelope env) {
        try {
            BistroClient client = ClientSession.getClient();
            if (client != null && client.isConnected()) {
                client.sendToServer(new KryoMessage("ENVELOPE", KryoUtil.toBytes(env)));
            } else {
                showAlert("Connection error", "not connected to server");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    private Envelope unwrapToEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope e) return e;
            if (msg instanceof KryoMessage km) return (Envelope) KryoUtil.fromBytes(km.getPayload());
        } catch (Exception e) { e.printStackTrace();}
        return null;
    }
}