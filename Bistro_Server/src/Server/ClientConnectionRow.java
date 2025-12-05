package Server;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Row בתבלת החיבורים של השרת.
 */
public class ClientConnectionRow {

    private final StringProperty hostName = new SimpleStringProperty();
    private final StringProperty ipAddress = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public ClientConnectionRow(String hostName, String ipAddress, String status) {
        this.hostName.set(hostName);
        this.ipAddress.set(ipAddress);
        this.status.set(status);
    }

    public String getHostName() { return hostName.get(); }
    public void setHostName(String value) { hostName.set(value); }
    public StringProperty hostNameProperty() { return hostName; }

    public String getIpAddress() { return ipAddress.get(); }
    public void setIpAddress(String value) { ipAddress.set(value); }
    public StringProperty ipAddressProperty() { return ipAddress; }

    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }
}

