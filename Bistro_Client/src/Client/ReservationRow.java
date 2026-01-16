package Client;

import javafx.beans.property.*;

/**
 * Represents a single reservation row displayed in JavaFX TableView components.
 * <p>
 * This class is a UI model that wraps reservation data using JavaFX properties,
 * enabling automatic binding and updates within table views.
 * </p>
 */
public class ReservationRow {

    private final IntegerProperty reservationId = new SimpleIntegerProperty();
    private final StringProperty confirmationCode = new SimpleStringProperty();
    private final StringProperty reservationTime = new SimpleStringProperty();
    private final StringProperty expiryTime = new SimpleStringProperty();
    private final IntegerProperty numOfCustomers = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    /**
     * Constructs a reservation row with all required display fields.
     *
     * @param reservationId     unique reservation identifier
     * @param confirmationCode reservation confirmation code
     * @param reservationTime  scheduled reservation time (formatted string)
     * @param expiryTime       reservation expiration time (formatted string)
     * @param numOfCustomers   number of customers for the reservation
     * @param status           current reservation status
     */
    public ReservationRow(int reservationId,
                          String confirmationCode,
                          String reservationTime,
                          String expiryTime,
                          int numOfCustomers,
                          String status) {
        this.reservationId.set(reservationId);
        this.confirmationCode.set(confirmationCode);
        this.reservationTime.set(reservationTime);
        this.expiryTime.set(expiryTime);
        this.numOfCustomers.set(numOfCustomers);
        this.status.set(status);
    }

    /** @return reservation ID value */
    public int getReservationId() { return reservationId.get(); }
    /** @return reservation ID property for table binding */
    public IntegerProperty reservationIdProperty() { return reservationId; }
    /** @return confirmation code value */
    public String getConfirmationCode() { return confirmationCode.get(); }

    /** @return confirmation code property for table binding */
    public StringProperty confirmationCodeProperty() { return confirmationCode; }
    /** @return reservation time string */
    public String getReservationTime() { return reservationTime.get(); }

    /** @return reservation time property for table binding */
    public StringProperty reservationTimeProperty() { return reservationTime; }
    /** @return expiration time string */
    public String getExpiryTime() { return expiryTime.get(); }
    /** @return expiration time property for table binding */
    public StringProperty expiryTimeProperty() { return expiryTime; }

    /** @return number of customers */
    public int getNumOfCustomers() { return numOfCustomers.get(); }

    /** @return number of customers property for table binding */
    public IntegerProperty numOfCustomersProperty() { return numOfCustomers; }
    /** @return reservation status */
    public String getStatus() { return status.get(); }
    /** @return reservation status property for table binding */
    public StringProperty statusProperty() { return status; }
    /**
     * Updates the reservation status value.
     *
     * @param s new status string
     */
    public void setStatus(String s) { status.set(s); }
}


