package Client;

import javafx.beans.property.*;

public class HistoryRow {
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty time = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty details = new SimpleStringProperty();
    private final StringProperty amount = new SimpleStringProperty();

    public HistoryRow(String date, String time, String type, String details, String amount) {
        this.date.set(date);
        this.time.set(time);
        this.type.set(type);
        this.details.set(details);
        this.amount.set(amount);
    }

    public StringProperty dateProperty() { return date; }
    public StringProperty timeProperty() { return time; }
    public StringProperty typeProperty() { return type; }
    public StringProperty detailsProperty() { return details; }
    public StringProperty amountProperty() { return amount; }
}

