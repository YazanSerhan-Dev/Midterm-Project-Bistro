package manager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class ManagerController {

    @FXML
    private Label lblTitle;

    @FXML
    private TableView<?> tblMain;

    // ===== Agent actions =====

    @FXML
    private void onViewWaitingList(ActionEvent event) {
        lblTitle.setText("Waiting List");
        System.out.println("Manager: View Waiting List");
    }

    @FXML
    private void onViewReservations(ActionEvent event) {
        lblTitle.setText("Reservations");
        System.out.println("Manager: View Reservations");
    }

    @FXML
    private void onViewSubscribers(ActionEvent event) {
        lblTitle.setText("Subscribers");
        System.out.println("Manager: View Subscribers");
    }

    @FXML
    private void onViewCurrentDiners(ActionEvent event) {
        lblTitle.setText("Current Diners");
        System.out.println("Manager: View Current Diners");
    }

    @FXML
    private void onRegisterCustomer(ActionEvent event) {
        System.out.println("Manager: Register Customer");
    }

    @FXML
    private void onUpdateTables(ActionEvent event) {
        System.out.println("Manager: Update Tables");
    }

    @FXML
    private void onUpdateOpeningHours(ActionEvent event) {
        System.out.println("Manager: Update Opening Hours");
    }

    // ===== Manager-only actions =====

    @FXML
    private void onViewReports(ActionEvent event) {
        lblTitle.setText("Reports");
        System.out.println("Manager: View Reports");
    }

    @FXML
    private void onViewStatistics(ActionEvent event) {
        lblTitle.setText("Statistics");
        System.out.println("Manager: View Statistics");
    }
}
