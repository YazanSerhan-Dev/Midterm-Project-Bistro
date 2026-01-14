package Client;

import javafx.beans.property.*;

public class SubscriberRow {

    private final StringProperty subscriberId = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty birthDate = new SimpleStringProperty(); // ✅ Added Property

    public SubscriberRow(String subscriberId,
                         String fullName,
                         String phone,
                         String email,
                         String birthDate) {
        this.subscriberId.set(subscriberId);
        this.fullName.set(fullName);
        this.phone.set(phone);
        this.email.set(email);
        this.birthDate.set(birthDate);
    }

    public String getSubscriberId() { return subscriberId.get(); }
    public StringProperty subscriberIdProperty() { return subscriberId; }

    public String getFullName() { return fullName.get(); }
    public StringProperty fullNameProperty() { return fullName; }

    public String getPhone() { return phone.get(); }
    public StringProperty phoneProperty() { return phone; }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }

    public String getBirthDate() { return birthDate.get(); }
    public StringProperty birthDateProperty() { return birthDate; }
}
