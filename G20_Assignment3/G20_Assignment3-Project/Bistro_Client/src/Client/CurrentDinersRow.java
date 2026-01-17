package Client;

import javafx.beans.property.*;
/**
 * Represents a single row in the "Current Diners" table.
 * <p>
 * This class is used as a JavaFX model object and exposes its fields
 * as observable properties to allow binding with {@link javafx.scene.control.TableView}.
 * </p>
 */
public class CurrentDinersRow {

    private final IntegerProperty tableNumber = new SimpleIntegerProperty();
    private final StringProperty customerName = new SimpleStringProperty();
    private final IntegerProperty peopleCount = new SimpleIntegerProperty();
    private final StringProperty checkInTime = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    /**
     * Creates a new row representing diners currently seated in the restaurant.
     *
     * @param tableNumber  table number assigned to the diners
     * @param customerName name of the customer
     * @param peopleCount  number of people at the table
     * @param checkInTime  check-in time as a formatted string
     * @param status       current dining status
     */
    public CurrentDinersRow(int tableNumber,
                           String customerName,
                           int peopleCount,
                           String checkInTime,
                           String status) {
        this.tableNumber.set(tableNumber);
        this.customerName.set(customerName);
        this.peopleCount.set(peopleCount);
        this.checkInTime.set(checkInTime);
        this.status.set(status);
    }
    /**
     * Returns the table number.
     *
     * @return table number
     */
    public int getTableNumber() { return tableNumber.get(); }
    /**
     * Returns the table number property.
     *
     * @return table number property
     */
    public IntegerProperty tableNumberProperty() { return tableNumber; }
    /**
     * Returns the customer name.
     *
     * @return customer name
     */
    public String getCustomerName() { return customerName.get(); }
    /**
     * Returns the customer name property.
     *
     * @return customer name property
     */
    public StringProperty customerNameProperty() { return customerName; }
    /**
     * Returns the number of people at the table.
     *
     * @return number of people
     */
    public int getPeopleCount() { return peopleCount.get(); }
    /**
     * Returns the people count property.
     *
     * @return people count property
     */
    public IntegerProperty peopleCountProperty() { return peopleCount; }
    /**
     * Returns the check-in time.
     *
     * @return check-in time string
     */
    public String getCheckInTime() { return checkInTime.get(); }
    /**
     * Returns the check-in time property.
     *
     * @return check-in time property
     */
    public StringProperty checkInTimeProperty() { return checkInTime; }
    /**
     * Returns the current dining status.
     *
     * @return dining status
     */
    public String getStatus() { return status.get(); }

    /**
     * Returns the status property.
     *
     * @return status property
     */
    public StringProperty statusProperty() { return status; }
}
