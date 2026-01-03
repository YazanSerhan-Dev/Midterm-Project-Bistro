package agent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import Client.ReservationRow;
import Client.WaitingListRow;
import Client.SubscriberRow;
import Client.CurrentDinersRow;
import common.Envelope;
import common.OpCode;
import common.dto.AgentSeatWaitingListDTO;
import Client.BistroClient;
import Client.ClientController;
import javafx.collections.FXCollections;

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

    
    @FXML
    private void onViewWaitingList(ActionEvent event) {
        lblTitle.setText("Waiting List");
        showWaitingListTable();
    }

    @FXML
    private void onViewReservations(ActionEvent event) {
        lblTitle.setText("Reservations");
        showReservationsTable();
    }

    @FXML
    private void onViewSubscribers(ActionEvent event) {
        lblTitle.setText("Subscribers");
        showSubscribersTable();
    }

    @FXML
    private void onViewCurrentDiners(ActionEvent event) {
        lblTitle.setText("Current Diners");
        showCurrentDinersTable();
    }

    @FXML
    private void onRegisterCustomer(ActionEvent event) {
        System.out.println("Agent: Register Customer");
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

        // Action: Seat
        TableColumn<WaitingListRow, Void> colSeat = new TableColumn<>("Seat");
        colSeat.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Seat");

            {
                btn.setOnAction(event -> {
                    WaitingListRow row = getTableView().getItems().get(getIndex());
                    AgentSeatWaitingListDTO dto =
                            new AgentSeatWaitingListDTO(row.getConfirmationCode());

                    Envelope env =
                            Envelope.request(
                                    OpCode.REQUEST_AGENT_SEAT_WAITING_LIST,
                                    dto
                            );

                    try {
                        clientController.getClient().sendToServer(env);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Action: No-Show
        TableColumn<WaitingListRow, Void> colNoShow = new TableColumn<>("No-Show");
        colNoShow.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("No-Show");

            {
                btn.setOnAction(event -> {
                    WaitingListRow row = getTableView().getItems().get(getIndex());
                    System.out.println("Marking no-show: " + row.getName());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Add all columns (NOW variables exist)
        tblWaitingList.getColumns().addAll(
                colPosition, colName, colPhone, colPeople, colStatus,
                colSeat, colNoShow
        );

        tblWaitingList.setItems(FXCollections.observableArrayList(
        	    new WaitingListRow(1, "Ahmad", "050-1234567", 4, "Waiting", "ABC123"),
        	    new WaitingListRow(2, "Sara", "052-7654321", 2, "Waiting", "DEF456"),
        	    new WaitingListRow(3, "Omar", "054-9998887", 3, "Seated", "GHI789")
        	));

    }

    private void showReservationsTable() {
        hideAllTables();

        tblReservations.setVisible(true);
        tblReservations.setManaged(true);

        tblReservations.getColumns().clear();
        tblReservations.getItems().clear();

        TableColumn<ReservationRow, Integer> colId =
                new TableColumn<>("Reservation ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("reservationId"));

        TableColumn<ReservationRow, String> colCode =
                new TableColumn<>("Confirmation Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));

        TableColumn<ReservationRow, String> colTime =
                new TableColumn<>("Reservation Time");
        colTime.setCellValueFactory(new PropertyValueFactory<>("reservationTime"));

        TableColumn<ReservationRow, String> colExpiry =
                new TableColumn<>("Expiry Time");
        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryTime"));

        TableColumn<ReservationRow, Integer> colCustomers =
                new TableColumn<>("Customers");
        colCustomers.setCellValueFactory(new PropertyValueFactory<>("numOfCustomers"));

        TableColumn<ReservationRow, String> colStatus =
                new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
     // Action: Seat reservation
        TableColumn<ReservationRow, Void> colSeat = new TableColumn<>("Seat");
        colSeat.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Seat");

            {
                btn.setOnAction(event -> {
                    ReservationRow row = getTableView().getItems().get(getIndex());
                    System.out.println(
                        "Seating reservation. Code: " + row.getConfirmationCode()
                    );
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Action: Cancel reservation
        TableColumn<ReservationRow, Void> colCancel = new TableColumn<>("Cancel");
        colCancel.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Cancel");

            {
                btn.setOnAction(event -> {
                    ReservationRow row = getTableView().getItems().get(getIndex());
                    System.out.println(
                        "Cancelling reservation. Code: " + row.getConfirmationCode()
                    );
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });


        tblReservations.getColumns().addAll(
        		colId, colCode, colTime, colExpiry, colCustomers, colStatus,
                colSeat, colCancel
        );

        tblReservations.setItems(FXCollections.observableArrayList(
                new ReservationRow(101, "ABC123", "2025-02-10 18:00", "2025-02-10 18:30", 4, "ACTIVE"),
                new ReservationRow(102, "XYZ456", "2025-02-10 19:00", "2025-02-10 19:30", 2, "ACTIVE"),
                new ReservationRow(103, "LMN789", "2025-02-11 20:00", "2025-02-11 20:30", 5, "CANCELLED")
        ));
    }
    
    private void showSubscribersTable() {
    	tblSubscibers.getColumns().clear();
    	tblSubscibers.getItems().clear();

        TableColumn<SubscriberRow, Integer> colId =
                new TableColumn<>("Subscriber ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("subscriberId"));

        TableColumn<SubscriberRow, String> colName =
                new TableColumn<>("Full Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<SubscriberRow, String> colPhone =
                new TableColumn<>("Phone");
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<SubscriberRow, String> colEmail =
                new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<SubscriberRow, String> colStatus =
                new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblSubscibers.getColumns().addAll(
                colId, colName, colPhone, colEmail, colStatus
        );

        tblSubscibers.setItems(FXCollections.observableArrayList(
                new SubscriberRow(201, "Ahmad Ali", "050-1112222", "ahmad@mail.com", "ACTIVE"),
                new SubscriberRow(202, "Sara Cohen", "052-3334444", "sara@mail.com", "FROZEN"),
                new SubscriberRow(203, "Omar Hassan", "054-5556666", "omar@mail.com", "ACTIVE")
        ));
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
     // Action: Finish dining
        TableColumn<CurrentDinersRow, Void> colFinish = new TableColumn<>("Finish");
        colFinish.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Finish");

            {
                btn.setOnAction(event -> {
                    CurrentDinersRow row = getTableView().getItems().get(getIndex());
                    System.out.println(
                        "Finishing dining. Table: " + row.getTableNumber()
                    );
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });


        tblCurrentDiners.getColumns().addAll(
        		colTable, colName, colPeople, colCheckIn, colStatus,
                colFinish
        );

        tblCurrentDiners.setItems(FXCollections.observableArrayList(
                new CurrentDinersRow(5, "Ahmad Ali", 4, "18:05", "DINING"),
                new CurrentDinersRow(8, "Sara Cohen", 2, "18:20", "DINING"),
                new CurrentDinersRow(12, "Omar Hassan", 6, "19:00", "DINING")
        ));
    }



}