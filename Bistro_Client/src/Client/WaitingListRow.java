package Client;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class WaitingListRow {

    private final SimpleIntegerProperty position;
    private final SimpleStringProperty name;
    private final SimpleStringProperty phone;
    private final SimpleIntegerProperty peopleCount;
    private final SimpleStringProperty status;
    private final SimpleStringProperty confirmationCode;

    public WaitingListRow(int position,
                          String name,
                          String phone,
                          int peopleCount,
                          String status,
                          String confirmationCode) {

        this.position = new SimpleIntegerProperty(position);
        this.name = new SimpleStringProperty(name);
        this.phone = new SimpleStringProperty(phone);
        this.peopleCount = new SimpleIntegerProperty(peopleCount);
        this.status = new SimpleStringProperty(status);
        this.confirmationCode = new SimpleStringProperty(confirmationCode);
    }

    public int getPosition() {
        return position.get();
    }

    public String getName() {
        return name.get();
    }

    public String getPhone() {
        return phone.get();
    }

    public int getPeopleCount() {
        return peopleCount.get();
    }

    public String getStatus() {
        return status.get();
    }

    public String getConfirmationCode() {
        return confirmationCode.get();
    }
}
