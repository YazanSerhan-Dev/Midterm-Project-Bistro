package Client;

import javafx.beans.property.*;

/**
 * JavaFX row-model for displaying Subscriber data in a TableView.
 * <p>
 * This class wraps subscriber fields using JavaFX {@link Property} types so the UI can:
 * <ul>
 *   <li>Bind table columns to properties</li>
 *   <li>Support automatic refresh if values change (if you later add setters)</li>
 * </ul>
 * </p>
 *
 * Typical usage:
 * <pre>
 * TableColumn&lt;SubscriberRow, String&gt; col = new TableColumn<>("Email");
 * col.setCellValueFactory(new PropertyValueFactory<>("email"));
 * </pre>
 */
public class SubscriberRow {
	 /** Unique subscriber identifier (username / subscriber id). */
    private final StringProperty subscriberId = new SimpleStringProperty();
    /** Subscriber full name for display purposes. */

    private final StringProperty fullName = new SimpleStringProperty();
    /** Subscriber phone number (as string to preserve formatting/leading zeros). */
    private final StringProperty phone = new SimpleStringProperty();
    /** Subscriber email address. */
    private final StringProperty email = new SimpleStringProperty();
    /** Subscriber birth date (stored as formatted string for direct UI display). */
    private final StringProperty birthDate = new SimpleStringProperty(); // ✅ Added Property
    /**
     * Creates a new {@code SubscriberRow} instance for TableView display.
     *
     * @param subscriberId subscriber unique id/username
     * @param fullName     subscriber full name
     * @param phone        subscriber phone number
     * @param email        subscriber email address
     * @param birthDate    birth date formatted as a string (e.g., "2001-05-12")
     */
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
    /**
     * @return subscriber id value (String)
     */
    public String getSubscriberId() { return subscriberId.get(); }
    /**
     * @return JavaFX property for subscriber id (used by TableView binding)
     */
    public StringProperty subscriberIdProperty() { return subscriberId; }

    /**
     * @return full name value (String)
     */
    public String getFullName() { return fullName.get(); }
    /**
     * @return JavaFX property for full name
     */
    public StringProperty fullNameProperty() { return fullName; }
    /**
     * @return phone value (String)
     */
    public String getPhone() { return phone.get(); }
    /**
     * @return JavaFX property for phone
     */
    public StringProperty phoneProperty() { return phone; }
    /**
     * @return email value (String)
     */
    public String getEmail() { return email.get(); }
    /**
     * @return JavaFX property for email
     */
    public StringProperty emailProperty() { return email; }

    /**
     * @return birth date value (String)
     */
    public String getBirthDate() { return birthDate.get(); }
    /**
     * @return JavaFX property for birth date
     */
    public StringProperty birthDateProperty() { return birthDate; }
}
