package Client;

import javafx.beans.property.*;

public class CurrentDinersRow {

    private final IntegerProperty tableNumber = new SimpleIntegerProperty();
    private final StringProperty customerName = new SimpleStringProperty();
    private final IntegerProperty peopleCount = new SimpleIntegerProperty();
    private final StringProperty checkInTime = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

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

    public int getTableNumber() { return tableNumber.get(); }
    public IntegerProperty tableNumberProperty() { return tableNumber; }

    public String getCustomerName() { return customerName.get(); }
    public StringProperty customerNameProperty() { return customerName; }

    public int getPeopleCount() { return peopleCount.get(); }
    public IntegerProperty peopleCountProperty() { return peopleCount; }

    public String getCheckInTime() { return checkInTime.get(); }
    public StringProperty checkInTimeProperty() { return checkInTime; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
}
