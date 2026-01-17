package Server;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a single row in the server's client connections table.
 * <p>
 * Stores basic connection information for display purposes:
 * host name, IP address, and current connection status.
 */
public class ClientConnectionRow {

    /** Client machine host name (for display in the table). */
    private final StringProperty hostName = new SimpleStringProperty();

    /** Client IP address. */
    private final StringProperty ipAddress = new SimpleStringProperty();

    /** Connection status (e.g. "connected", "Disconnected"). */
    private final StringProperty status = new SimpleStringProperty();

    /**
     * Creates a row with the given host, IP and status.
     */
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


