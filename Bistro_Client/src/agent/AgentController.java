package agent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import Client.ReservationRow;
import Client.WaitingListRow;
import Client.SubscriberRow;
import Client.CurrentDinersRow;
import common.Envelope;
import common.OpCode;
import common.dto.RegistrationDTO;
import Client.BistroClient;
import Client.ClientController;
import javafx.collections.FXCollections;

import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;

import javafx.scene.control.PasswordField;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AgentController {

    @FXML
    private Label lblTitle;

    @FXML
    private TableView<WaitingListRow> tblWaitingList;

    @FXML
    private TableView<ReservationRow> tblReservations;

    @FXML
    private TableView<SubscriberRow> tblSubscibers;
    @FXML
    private TableView<CurrentDinersRow> tblCurrentDiners;
    
    private ClientController clientController;
    
    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    }
    
    @FXML private VBox paneRegisterSubscriber;
    @FXML private TextField txtSubName;
    @FXML private TextField txtSubPhone;
    @FXML private TextField txtSubEmail;


    
    @FXML
    private void onViewWaitingList(ActionEvent event) {
        lblTitle.setText("Waiting List");
        showWaitingListTable();
    }

    @FXML
    private void onViewReservations(ActionEvent event) {
        lblTitle.setText("Reservations");
        setupReservationsColumns();
        hideAllTables();
        tblReservations.setVisible(true);
        tblReservations.setManaged(true);
        tblReservations.getItems().clear();

        // USE THE NEW OPCODE
        Envelope env = Envelope.request(OpCode.REQUEST_AGENT_RESERVATIONS_LIST, null);
        
        try {
            if (clientController != null) {
                clientController.getClient().sendToServer(env);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   
    @FXML
    private void onViewCurrentDiners(ActionEvent event) {
        lblTitle.setText("Current Diners");
        showCurrentDinersTable();
    }

    @FXML
    private void onRegisterCustomer(ActionEvent event) {
        // 1. Create a custom Dialog
        Dialog<RegistrationDTO> dialog = new Dialog<>();
        dialog.setTitle("Register New Customer");
        dialog.setHeaderText("Enter Customer Details");

        // 2. Set the button types
        ButtonType registerButtonType = new ButtonType("Register", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        // 3. Create the layout and fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Password");

        TextField txtName = new TextField();
        txtName.setPromptText("Full Name");

        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Phone");

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email");

        // --- REMOVED Member Code and Barcode inputs --- 

        DatePicker datePicker = new DatePicker();

        grid.add(new Label("Username:"), 0, 0);
        grid.add(txtUsername, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(txtPassword, 1, 1);
        grid.add(new Label("Full Name:"), 0, 2);
        grid.add(txtName, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(txtPhone, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(txtEmail, 1, 4);
        
        // Adjusted row index for Date since we removed 2 rows
        grid.add(new Label("Birth Date:"), 0, 5); 
        grid.add(datePicker, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // 4. Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                String dob = "";
                if (datePicker.getValue() != null) {
                    dob = datePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                
                return new RegistrationDTO(
                    txtUsername.getText(),
                    txtPassword.getText(),
                    txtName.getText(),
                    txtPhone.getText(),
                    txtEmail.getText(),
                    null, // Member Code (Server will generate)
                    null, // Barcode (Server will generate)
                    dob
                );
            }
            return null;
        });

        // 5. Show dialog and send request
        dialog.showAndWait().ifPresent(dto -> {
            Envelope env = Envelope.request(OpCode.REQUEST_REGISTER_CUSTOMER, dto);
            try {
                if (clientController != null) {
                    clientController.getClient().sendToServer(env);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    @FXML
    private void onCancelRegisterSubscriber() {
        paneRegisterSubscriber.setVisible(false);
        paneRegisterSubscriber.setManaged(false);
        lblTitle.setText("Welcome Agent");
    }
    
    @FXML
    private void onUpdateTables(ActionEvent event) {
        System.out.println("Agent: Update Tables");
    }

    @FXML
    private void onUpdateOpeningHours(ActionEvent event) {
        System.out.println("Agent: Update Opening Hours");
    }

    private void hideAllTables() {
        tblWaitingList.setVisible(false);
        tblWaitingList.setManaged(false);

        tblReservations.setVisible(false);
        tblReservations.setManaged(false);
        
        tblSubscibers.setVisible(false);
        tblSubscibers.setManaged(false);

        
        tblCurrentDiners.setVisible(false);
        tblCurrentDiners.setManaged(false);
       
    }
    
    private void showWaitingListTable() {
        hideAllTables();

        tblWaitingList.setVisible(true);
        tblWaitingList.setManaged(true);

        tblWaitingList.getColumns().clear();
        tblWaitingList.getItems().clear();

        // Data columns
        TableColumn<WaitingListRow, Integer> colPosition =
                new TableColumn<>("Position");
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));

        TableColumn<WaitingListRow, String> colName =
                new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<WaitingListRow, String> colPhone =
                new TableColumn<>("Phone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<WaitingListRow, Integer> colPeople =
                new TableColumn<>("People");
        colPeople.setCellValueFactory(new PropertyValueFactory<>("peopleCount"));

        TableColumn<WaitingListRow, String> colStatus =
                new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        

       

        
        // Add all columns (NOW variables exist)
        tblWaitingList.getColumns().addAll(
                colPosition, colName, colPhone, colPeople, colStatus
                
        );

        tblWaitingList.setItems(FXCollections.observableArrayList(
        	    new WaitingListRow(1, "Ahmad", "050-1234567", 4, "Waiting", "ABC123"),
        	    new WaitingListRow(2, "Sara", "052-7654321", 2, "Waiting", "DEF456"),
        	    new WaitingListRow(3, "Omar", "054-9998887", 3, "Seated", "GHI789")
        	));

    }

 // Helper to setup columns (Cleaner code)
    private void setupReservationsColumns() {
        tblReservations.getColumns().clear();

        TableColumn<ReservationRow, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));

        TableColumn<ReservationRow, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));

        TableColumn<ReservationRow, String> colTime = new TableColumn<>("Reservation Time");
        colTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));

        TableColumn<ReservationRow, String> colExpiry = new TableColumn<>("Expiry Time");
        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryTime"));

        TableColumn<ReservationRow, Integer> colCustomers = new TableColumn<>("Guests");
        colCustomers.setCellValueFactory(new PropertyValueFactory<>("numOfCustomers"));

        TableColumn<ReservationRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblReservations.getColumns().addAll(colId, colCode, colTime, colExpiry, colCustomers, colStatus);
    }

    // Method called by ClientController when data arrives
    public void updateReservationsTable(List<?> data) {
        javafx.application.Platform.runLater(() -> {
            tblReservations.getItems().clear();
            
            for (Object obj : data) {
                if (obj instanceof common.dto.ReservationDTO dto) {
                    tblReservations.getItems().add(new ReservationRow(
                        dto.getReservationId(),
                        dto.getConfirmationCode(),
                        dto.getReservationTime(),
                        dto.getExpiryTime(),
                        dto.getNumOfCustomers(),
                        dto.getStatus()
                    ));
                }
            }
        });
    }
    
 // 1. Change the button action to REQUEST data instead of showing dummy data
    @FXML
    private void onViewSubscribers(ActionEvent event) {
        lblTitle.setText("Subscribers");
        
        // Setup columns (only need to do this once, but it's safe here)
        setupSubscriberColumns(); 
        
        // Show the table (it will be empty initially)
        hideAllTables();
        tblSubscibers.setVisible(true);
        tblSubscibers.setManaged(true);
        tblSubscibers.getItems().clear(); // Clear old data

        // Send Request to Server
        Envelope env = Envelope.request(OpCode.REQUEST_SUBSCRIBERS_LIST, null);
        try {
            if (clientController != null) {
                clientController.getClient().sendToServer(env);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. Helper to setup columns (moved out for clarity)
    private void setupSubscriberColumns() {
        tblSubscibers.getColumns().clear();
        
        TableColumn<SubscriberRow, Integer> colId = new TableColumn<>("Subscriber ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));

        TableColumn<SubscriberRow, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<SubscriberRow, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<SubscriberRow, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<SubscriberRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblSubscibers.getColumns().addAll(colId, colName, colPhone, colEmail, colStatus);
    }

    // 3. New method called by ClientController when data arrives
    public void updateSubscribersTable(List<?> data) {
        javafx.application.Platform.runLater(() -> {
            tblSubscibers.getItems().clear();
            
            for (Object obj : data) {
                if (obj instanceof common.dto.SubscriberDTO dto) {
                    // Convert DTO to UI Row
                    tblSubscibers.getItems().add(new SubscriberRow(
                        dto.getId(), 
                        dto.getFullName(), 
                        dto.getPhone(), 
                        dto.getEmail(), 
                        dto.getStatus()
                    ));
                }
            }
        });
    }
    
    private void showCurrentDinersTable() {
        hideAllTables();

        tblCurrentDiners.setVisible(true);
        tblCurrentDiners.setManaged(true);

        tblCurrentDiners.getColumns().clear();
        tblCurrentDiners.getItems().clear();

        TableColumn<CurrentDinersRow, Integer> colTable =
                new TableColumn<>("Table");
        colTable.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));

        TableColumn<CurrentDinersRow, String> colName =
                new TableColumn<>("Customer");
        colName.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<CurrentDinersRow, Integer> colPeople =
                new TableColumn<>("People");
        colPeople.setCellValueFactory(new PropertyValueFactory<>("peopleCount"));

        TableColumn<CurrentDinersRow, String> colCheckIn =
                new TableColumn<>("Check-in Time");
        colCheckIn.setCellValueFactory(new PropertyValueFactory<>("checkInTime"));

        TableColumn<CurrentDinersRow, String> colStatus =
                new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
     
        tblCurrentDiners.getColumns().addAll(
        		colTable, colName, colPeople, colCheckIn, colStatus
        );

        tblCurrentDiners.setItems(FXCollections.observableArrayList(
                new CurrentDinersRow(5, "Ahmad Ali", 4, "18:05", "DINING"),
                new CurrentDinersRow(8, "Sara Cohen", 2, "18:20", "DINING"),
                new CurrentDinersRow(12, "Omar Hassan", 6, "19:00", "DINING")
        ));
    }
    
   

   

  




}