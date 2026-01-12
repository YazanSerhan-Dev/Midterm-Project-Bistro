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
import common.dto.OpeningHoursDTO;
import common.dto.RegistrationDTO;
import common.dto.ReservationDTO;
import common.dto.SubscriberDTO;
import common.dto.WaitingListDTO;
import common.dto.RestaurantTableDTO;
import common.dto.LoginRequestDTO;
import common.dto.LoginResponseDTO;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import common.dto.ReportDTO;
import javafx.scene.chart.NumberAxis;
import javafx.util.StringConverter;

public class StaffController implements ClientUI {

    @FXML private Label lblTitle;
    @FXML private Label lblStatus;

    // Tables
    @FXML private TableView<WaitingListRow> tblWaitingList;
    @FXML private TableView<ReservationRow> tblReservations;
    @FXML private TableView<SubscriberRow> tblSubscribers;
    @FXML private TableView<CurrentDinersRow> tblCurrentDiners;
    
    // Manager Views
    //@FXML private VBox paneReports; to check if to delete
    @FXML private VBox paneWaitingList;
    @FXML private Button btnViewReports;
    
    @FXML private VBox paneTables;
    @FXML private TableView<RestaurantTableDTO> tblRestaurantTables;
    @FXML private TextField tfTableId;
    @FXML private TextField tfTableSeats;
    
    @FXML private VBox paneOpeningHours;
    
    @FXML private TableView<OpeningHoursDTO> tblRegularHours; // ✅ NEW Table
    @FXML private Label lblSelectedRegularDay;                // ✅ NEW Label
    @FXML private TextField tfRegularOpen;
    @FXML private TextField tfRegularClose;

    // Special Table & Inputs
    @FXML private TableView<OpeningHoursDTO> tblSpecialHours;
    @FXML private DatePicker dpSpecialDate;
    @FXML private TextField tfSpecialOpen;
    @FXML private TextField tfSpecialClose;
    @FXML private Label lblTodayHours; 
    //manager reports
    @FXML private VBox paneReports;
    @FXML private BarChart<String, Number> chartPerformance;
    @FXML private BarChart<String, Number> chartActivity;
    @FXML private ComboBox<String> cbReportMonth;
    @FXML private ComboBox<Integer> cbReportYear;
    @FXML private Label lblManagerSection;
    
    // ✅ REMOVED: private ReservationFormController reservationFormController; 

    @FXML
    public void initialize() {

        ClientSession.bindUI(this);
        String role = ClientSession.getRole();
        boolean isManager = role != null && role.trim().equalsIgnoreCase("MANAGER");
        String username = ClientSession.getUsername();
        lblTitle.setText(role + " Dashboard: " + username);

    	System.out.println("StaffController role = [" + ClientSession.getRole() + "]");

        if (btnViewReports != null) {
            btnViewReports.setVisible(isManager);
            btnViewReports.setManaged(isManager);
        }
       
        if (lblManagerSection != null) {
            lblManagerSection.setVisible(isManager);
            lblManagerSection.setManaged(isManager);
        }
        
        // Setup Report Dropdowns
        if (cbReportMonth != null) {
            cbReportMonth.getItems().addAll(
                "January", "February", "March", "April", "May", "June", 
                "July", "August", "September", "October", "November", "December"
            );
            // Select current month
            cbReportMonth.getSelectionModel().select(java.time.LocalDate.now().getMonthValue() - 1);
        }
        
        if (cbReportYear != null) {
            int currentYear = java.time.LocalDate.now().getYear();
            cbReportYear.getItems().addAll(currentYear - 1, currentYear, currentYear + 1);
            cbReportYear.getSelectionModel().select(Integer.valueOf(currentYear));
        }

        // -------------------------------------------------------------
        // 1. ACTIVITY CHART (Reservations/Waiting) - Force Integer Axis
        // -------------------------------------------------------------
        if (chartActivity != null) {
        	chartActivity.setAnimated(false);
            NumberAxis yAxis = (NumberAxis) chartActivity.getYAxis();

            yAxis.setForceZeroInRange(true);
            yAxis.setMinorTickCount(0);
            yAxis.setTickUnit(1); // Step by 1

            yAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    double val = object.doubleValue();
                    if (Math.abs(val - Math.round(val)) < 0.1) {
                        return String.valueOf((int) Math.round(val));
                    }
                    return ""; 
                }

                @Override
                public Number fromString(String string) {
                    return Integer.parseInt(string);
                }
            });
        }

        // -------------------------------------------------------------
        // 2. PERFORMANCE CHART (Overstay) - DYNAMIC HEIGHT + Integer
        // -------------------------------------------------------------
        if (chartPerformance != null) {
        	chartPerformance.setAnimated(false);
        	NumberAxis yAxisPerf = (NumberAxis) chartPerformance.getYAxis();
            yAxisPerf.setAutoRanging(true); 
            yAxisPerf.setForceZeroInRange(true);
            yAxisPerf.setMinorTickCount(0);
            yAxisPerf.setTickUnit(1); 
            
            yAxisPerf.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    double val = object.doubleValue();
                    if (Math.abs(val - Math.round(val)) < 0.1) {
                        return String.valueOf((int) Math.round(val));
                    }
                    return "";
                }
                @Override
                public Number fromString(String string) { return Integer.parseInt(string); }
            });
        }

        onViewReservations(null);
        sendToServer(Envelope.request(OpCode.REQUEST_TODAY_HOURS, null));
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
                // --- Lists Updates ---
                case RESPONSE_AGENT_RESERVATIONS_LIST:
                    updateReservationsTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_SUBSCRIBERS_LIST:
                    updateSubscribersTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_WAITING_LIST:
                    updateWaitingListTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_CURRENT_DINERS:
                    updateCurrentDinersTable((List<?>) env.getPayload());
                    break;
                case RESPONSE_TABLES_GET:
                    updateRestaurantTables((List<?>) env.getPayload());
                    break;
                case RESPONSE_OPENING_HOURS_GET:
                    updateOpeningHours((List<?>) env.getPayload());
                    break;

                // --- Single Updates / Actions ---
                case RESPONSE_REGISTER_CUSTOMER:
                    showAlert("Success", "Customer registered successfully.");
                    break;
                case RESPONSE_TODAY_HOURS:
                    updateTodayHours((String) env.getPayload());
                    break;
                case RESPONSE_LOGIN_SUBSCRIBER:
                    handleSubscriberLoginResponse((LoginResponseDTO) env.getPayload());
                    break;

                // --- Operation Results (Alert + Refresh) ---
                case RESPONSE_WAITING_ADD:
                case RESPONSE_WAITING_ASSIGN:
                case RESPONSE_WAITING_REMOVE:
                    handleWaitingListUpdateResponse((String) env.getPayload());
                    break;

                case RESPONSE_TABLE_ADD:
                case RESPONSE_TABLE_REMOVE:
                case RESPONSE_TABLE_UPDATE:
                    handleTableUpdateResponse((String) env.getPayload());
                    break;

                case RESPONSE_OPENING_HOURS_UPDATE:
                case RESPONSE_OPENING_HOURS_ADD_SPECIAL:
                case RESPONSE_OPENING_HOURS_REMOVE:
                    handleOpeningHoursUpdateResponse((String) env.getPayload());
                    break;

                // --- Reports ---
                case RESPONSE_REPORT_PERFORMANCE:
                    populatePerformanceChart((List<ReportDTO>) env.getPayload());
                    break;
                case RESPONSE_REPORT_ACTIVITY:
                    populateActivityChart((List<ReportDTO>) env.getPayload());
                    break;

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
        paneReports.setVisible(true);
        paneReports.setManaged(true);
        
        onRefreshReports(null);
    }
    
    @FXML
    private void onRefreshReports(ActionEvent event) {
        // 1. Get selected values
        int monthIndex = cbReportMonth.getSelectionModel().getSelectedIndex(); // 0 = Jan
        Integer year = cbReportYear.getSelectionModel().getSelectedItem();
        
        if (monthIndex < 0 || year == null) {
            showAlert("Selection Error", "Please select both a month and a year.");
            return;
        }

        int month = monthIndex + 1; // Convert 0-11 to 1-12

        // 2. Create Request DTO
        common.dto.ReportRequestDTO req = new common.dto.ReportRequestDTO(month, year);
        
        // 3. Send Requests
        sendToServer(Envelope.request(OpCode.REQUEST_REPORT_PERFORMANCE, req));
        sendToServer(Envelope.request(OpCode.REQUEST_REPORT_ACTIVITY, req));
    }
    
 // 4. Helper to Fill Performance Chart
    private void populatePerformanceChart(List<ReportDTO> data) {
        chartPerformance.getData().clear();
        
        XYChart.Series<String, Number> seriesLate = new XYChart.Series<>();
        seriesLate.setName("Late Arrivals (min)");
        
        XYChart.Series<String, Number> seriesOver = new XYChart.Series<>();
        seriesOver.setName("Overstay (min)");

        for (ReportDTO d : data) {
            seriesLate.getData().add(new XYChart.Data<>(d.getDate(), d.getTotalLate()));
            seriesOver.getData().add(new XYChart.Data<>(d.getDate(), d.getTotalOverstay()));
        }
        
        chartPerformance.getData().addAll(seriesLate, seriesOver);
    }

    // 5. Helper to Fill Activity Chart
    private void populateActivityChart(List<ReportDTO> data) {
        chartActivity.getData().clear();
        
        XYChart.Series<String, Number> seriesRes = new XYChart.Series<>();
        seriesRes.setName("Reservations");
        
        XYChart.Series<String, Number> seriesWait = new XYChart.Series<>();
        seriesWait.setName("Waiting List");

        for (ReportDTO d : data) {
            seriesRes.getData().add(new XYChart.Data<>(d.getDate(), d.getTotalReservations()));
            seriesWait.getData().add(new XYChart.Data<>(d.getDate(), d.getTotalWaiting()));
        }
        
        chartActivity.getData().addAll(seriesRes, seriesWait);
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
        
        if (paneTables != null) { paneTables.setVisible(false); paneTables.setManaged(false); }

        if (paneOpeningHours != null) { paneOpeningHours.setVisible(false); paneOpeningHours.setManaged(false); }
        tblCurrentDiners.setVisible(false); tblCurrentDiners.setManaged(false);
        if(paneReports != null) { paneReports.setVisible(false); paneReports.setManaged(false); }
    }

    private void updateReservationsTable(List<?> data) {
        tblReservations.getItems().clear();
        
        for (Object obj : data) {
            if (obj instanceof common.dto.ReservationDTO dto) {
                // Here we use the NEW 8-argument constructor explicitly
                // This bypasses dtoToRow entirely for this specific table
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
    }

    private void updateSubscribersTable(List<?> data) {
        tblSubscribers.getItems().clear();
        
        for (Object obj : data) {
            if (obj instanceof common.dto.SubscriberDTO dto) {
                // ✅ Use existing "getName()" directly
                tblSubscribers.getItems().add(new SubscriberRow(
                    dto.getId(),
                    dto.getFullName(),  
                    dto.getPhone(),
                    dto.getEmail(),
                    dto.getStatus()
                ));
            }
        }
    }
    
    private void updateWaitingListTable(List<?> data) {
        tblWaitingList.getItems().clear();
        for (Object obj : data) {
            if (obj instanceof WaitingListDTO dto) {
                WaitingListRow row = new WaitingListRow(dto.getId(),dto.getPeopleCount(),dto.getRequestTime(),dto.getStatus(),dto.getConfirmationCode());
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
    
    private void updateRestaurantTables(List<?> data) {
        tblRestaurantTables.getItems().clear();
        for (Object o : data) {
            if (o instanceof RestaurantTableDTO dto) {
                tblRestaurantTables.getItems().add(dto);
            }
        }
    }

    private void updateOpeningHours(List<?> data) {
        tblRegularHours.getItems().clear();
        tblSpecialHours.getItems().clear();

        // Temporary list to help sort Regular days (Mon, Tue, Wed...)
        List<OpeningHoursDTO> regularList = new java.util.ArrayList<>();

        for (Object obj : data) {
            if (obj instanceof OpeningHoursDTO dto) {
                if (dto.isSpecial()) {
                    tblSpecialHours.getItems().add(dto);
                } else {
                    regularList.add(dto);
                }
            }
        }
        
        // Custom Sorter for Days of Week
        List<String> daysOrder = List.of("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
        regularList.sort((a, b) -> Integer.compare(daysOrder.indexOf(a.getDayOfWeek()), daysOrder.indexOf(b.getDayOfWeek())));

        tblRegularHours.getItems().addAll(regularList);
    }

    private void updateTodayHours(String hours) {
        if (hours != null) {
            lblTodayHours.setText(hours);
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
        colId.setPrefWidth(50);

       

        TableColumn<ReservationRow, String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));
        colTime.setPrefWidth(140);

        TableColumn<ReservationRow, Integer> colGuests = new TableColumn<>("Guests");
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numOfCustomers"));
        colGuests.setPrefWidth(60);

        TableColumn<ReservationRow, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        colCode.setPrefWidth(80);

        TableColumn<ReservationRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblReservations.getColumns().addAll(colId, colTime, colGuests, colCode, colStatus);
    }

    private void setupSubscriberColumns() {
        if (!tblSubscribers.getColumns().isEmpty()) return;

        TableColumn<SubscriberRow, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));
        colId.setPrefWidth(50);

        

        TableColumn<SubscriberRow, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPhone.setPrefWidth(120);

        TableColumn<SubscriberRow, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(180);

        TableColumn<SubscriberRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblSubscribers.getColumns().addAll(colId, colPhone, colEmail, colStatus);
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
    
    private void setupTableColumns() {
        if (!tblRestaurantTables.getColumns().isEmpty()) return;

        TableColumn<RestaurantTableDTO, String> colId = new TableColumn<>("Table ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("tableId"));

        TableColumn<RestaurantTableDTO, Integer> colSeats = new TableColumn<>("Seats");
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seats"));

        TableColumn<RestaurantTableDTO, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblRestaurantTables.getColumns().addAll(colId, colSeats, colStatus);
    }
    
    private void setupOpeningHoursTables() {
        // --- 1. SETUP REGULAR HOURS TABLE (Top) ---
        if (tblRegularHours.getColumns().isEmpty()) {
            TableColumn<OpeningHoursDTO, String> colDay = new TableColumn<>("Day");
            colDay.setCellValueFactory(new PropertyValueFactory<>("dayOfWeek"));
            colDay.setPrefWidth(120);

            TableColumn<OpeningHoursDTO, String> colOpen = new TableColumn<>("Open");
            colOpen.setCellValueFactory(new PropertyValueFactory<>("openTime"));

            TableColumn<OpeningHoursDTO, String> colClose = new TableColumn<>("Close");
            colClose.setCellValueFactory(new PropertyValueFactory<>("closeTime"));

            tblRegularHours.getColumns().addAll(colDay, colOpen, colClose);
            
            // ✅ LISTENER: When user clicks a row, fill the Edit Box
            tblRegularHours.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    lblSelectedRegularDay.setText(newVal.getDayOfWeek()); // Show "Monday"
                    tfRegularOpen.setText(newVal.getOpenTime());
                    tfRegularClose.setText(newVal.getCloseTime());
                } else {
                    lblSelectedRegularDay.setText("Select a day...");
                    tfRegularOpen.clear();
                    tfRegularClose.clear();
                }
            });
        }

        // --- 2. SETUP SPECIAL HOURS TABLE (Bottom) ---
        if (tblSpecialHours.getColumns().isEmpty()) {
            TableColumn<OpeningHoursDTO, String> colDate = new TableColumn<>("Date");
            colDate.setCellValueFactory(new PropertyValueFactory<>("specialDate"));
            colDate.setPrefWidth(120);

            TableColumn<OpeningHoursDTO, String> colOpenS = new TableColumn<>("Open");
            colOpenS.setCellValueFactory(new PropertyValueFactory<>("openTime"));

            TableColumn<OpeningHoursDTO, String> colCloseS = new TableColumn<>("Close");
            colCloseS.setCellValueFactory(new PropertyValueFactory<>("closeTime"));
            
            TableColumn<OpeningHoursDTO, String> colDayS = new TableColumn<>("Type");
            colDayS.setCellValueFactory(new PropertyValueFactory<>("dayOfWeek")); // Shows "Special"

            tblSpecialHours.getColumns().addAll(colDate, colOpenS, colCloseS, colDayS);
        }
    }

    
    
    
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
    
    @FXML
    private void onManageTables(ActionEvent event) {
        lblTitle.setText("Manage Restaurant Tables");
        hideAllViews();
        
        paneTables.setVisible(true);
        paneTables.setManaged(true);
        
        setupTableColumns();
        refreshTableList();
    }
    
    @FXML
    private void onAddTable(ActionEvent event) {
        String id = tfTableId.getText().trim();
        String seatsStr = tfTableSeats.getText().trim();

        if (id.isEmpty() || seatsStr.isEmpty()) {
            showAlert("Error", "Please enter Table ID and Seat count.");
            return;
        }

        try {
            int seats = Integer.parseInt(seatsStr);
            // Check if exists to determine if it's update or add (Simple approach: Just Try Add/Update)
            // Ideally, we check logic, but for now let's assume Add.
            // If you want Update logic, check if id exists in table list.
            
            RestaurantTableDTO dto = new RestaurantTableDTO(id, seats, "FREE");
            
            // Send ADD request (If it exists, SQL might throw error, or we can use Replace)
            sendToServer(Envelope.request(OpCode.REQUEST_TABLE_ADD, dto));
            
            tfTableId.clear();
            tfTableSeats.clear();

        } catch (NumberFormatException e) {
            showAlert("Error", "Seats must be a number.");
        }
    }

    @FXML
    private void onDeleteTable(ActionEvent event) {
        RestaurantTableDTO selected = tblRestaurantTables.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Select a table to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete table " + selected.getTableId() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                sendToServer(Envelope.request(OpCode.REQUEST_TABLE_REMOVE, selected.getTableId()));
            }
        });
    }
    
 // 1. Open the "Manage Opening Hours" Screen
    @FXML
    private void onManageOpeningHours(ActionEvent event) {
        lblTitle.setText("Manage Opening Hours");
        hideAllViews();
        paneOpeningHours.setVisible(true);
        paneOpeningHours.setManaged(true);
        
        // Change: Call the new UI setup method
        setupOpeningHoursTables();
        
        sendToServer(Envelope.request(OpCode.REQUEST_OPENING_HOURS_GET, null));
    }

    // 2. Button: Update Weekly Hour
    @FXML
    private void onUpdateRegularHour(ActionEvent event) {
        // 1. Get selected item from the TABLE
        OpeningHoursDTO selected = tblRegularHours.getSelectionModel().getSelectedItem();
        
        if (selected == null) { 
            showAlert("Error", "Please select a day from the table first."); 
            return; 
        }
        
        String newOpen = tfRegularOpen.getText().trim();
        String newClose = tfRegularClose.getText().trim();
        
        if (newOpen.isEmpty() || newClose.isEmpty()) {
            showAlert("Error", "Times cannot be empty.");
            return;
        }

        // 2. Update DTO
        selected.setOpenTime(newOpen);
        selected.setCloseTime(newClose);
        
        // 3. Send to Server
        sendToServer(Envelope.request(OpCode.REQUEST_OPENING_HOURS_UPDATE, selected));
    }

    // 3. Button: Add Special Date Exception
    @FXML
    private void onAddSpecialHour(ActionEvent event) {
        if (dpSpecialDate.getValue() == null) { 
            showAlert("Error", "Please pick a date."); 
            return; 
        }
        
        String dateStr = dpSpecialDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String open = tfSpecialOpen.getText().trim();
        String close = tfSpecialClose.getText().trim();
        
        if (open.isEmpty() || close.isEmpty()) { 
            showAlert("Error", "Please enter open and close times."); 
            return; 
        }

        // Create new DTO for special date
        // ID is 0 because it's new (DB will assign ID)
        OpeningHoursDTO dto = new OpeningHoursDTO(0, "Special", open, close, true, dateStr);
        
        sendToServer(Envelope.request(OpCode.REQUEST_OPENING_HOURS_ADD_SPECIAL, dto));
        
        // Clear inputs after sending
        dpSpecialDate.setValue(null);
        tfSpecialOpen.clear();
        tfSpecialClose.clear();
    }

    // 4. Button: Remove Special Date
    @FXML
    private void onRemoveSpecialHour(ActionEvent event) {
        OpeningHoursDTO selected = tblSpecialHours.getSelectionModel().getSelectedItem();
        if (selected == null) { 
            showAlert("Error", "Select a special date to remove."); 
            return; 
        }
        
        sendToServer(Envelope.request(OpCode.REQUEST_OPENING_HOURS_REMOVE, selected.getHoursId()));
    }

    private void refreshTableList() {
        sendToServer(Envelope.request(OpCode.REQUEST_TABLES_GET, null));
    }
    
    private void handleSubscriberLoginResponse(LoginResponseDTO res) {
        if (res.isOk()) {
            ClientSession.setRole("SUBSCRIBER");
            ClientSession.setUsername(res.getUsername());
            SceneManager.showCustomerMain();
        } else {
            showAlert("Validation Failed", res.getMessage());
        }
    }

    private void handleWaitingListUpdateResponse(String msg) {
        if (msg != null) showAlert("Success", msg);
        sendToServer(new Envelope(OpCode.REQUEST_WAITING_LIST));
    }

    private void handleTableUpdateResponse(String msg) {
        if (msg != null) showAlert("Success", msg);
        refreshTableList();
    }

    private void handleOpeningHoursUpdateResponse(String msg) {
        if (msg != null) showAlert("Success", msg);
        
        sendToServer(Envelope.request(OpCode.REQUEST_OPENING_HOURS_GET, null));
        
        sendToServer(Envelope.request(OpCode.REQUEST_TODAY_HOURS, null));
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