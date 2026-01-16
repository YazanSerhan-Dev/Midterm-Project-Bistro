package Client;

import javafx.beans.property.*;

public class ReservationRow {

    private final IntegerProperty reservationId = new SimpleIntegerProperty();
    private final StringProperty confirmationCode = new SimpleStringProperty();
    private final StringProperty reservationTime = new SimpleStringProperty();
    private final StringProperty expiryTime = new SimpleStringProperty();
    private final IntegerProperty numOfCustomers = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();

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

    public int getReservationId() { return reservationId.get(); }
    public IntegerProperty reservationIdProperty() { return reservationId; }

    public String getConfirmationCode() { return confirmationCode.get(); }
    public StringProperty confirmationCodeProperty() { return confirmationCode; }

    public String getReservationTime() { return reservationTime.get(); }
    public StringProperty reservationTimeProperty() { return reservationTime; }

    public String getExpiryTime() { return expiryTime.get(); }
    public StringProperty expiryTimeProperty() { return expiryTime; }

    public int getNumOfCustomers() { return numOfCustomers.get(); }
    public IntegerProperty numOfCustomersProperty() { return numOfCustomers; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public void setStatus(String s) { status.set(s); }
}


