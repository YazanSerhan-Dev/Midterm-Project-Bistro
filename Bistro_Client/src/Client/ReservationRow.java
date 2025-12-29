package Client;

import javafx.beans.property.*;

public class ReservationRow {
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty time = new SimpleStringProperty();
    private final IntegerProperty guests = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty price = new SimpleStringProperty();

    public ReservationRow(String code, String date, String time, int guests, String status, String price) {
        this.code.set(code);
        this.date.set(date);
        this.time.set(time);
        this.guests.set(guests);
        this.status.set(status);
        this.price.set(price);
    }

    public String getCode() { return code.get(); }
    public String getDate() { return date.get(); }
    public String getTime() { return time.get(); }
    public int getGuests() { return guests.get(); }
    public String getStatus() { return status.get(); }
    public String getPrice() { return price.get(); }

    public void setStatus(String s) { status.set(s); }

    public StringProperty codeProperty() { return code; }
    public StringProperty dateProperty() { return date; }
    public StringProperty timeProperty() { return time; }
    public IntegerProperty guestsProperty() { return guests; }
    public StringProperty statusProperty() { return status; }
    public StringProperty priceProperty() { return price; }
}

