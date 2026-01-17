package common.dto;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) representing a restaurant subscriber.
 * <p>
 * Used to transfer subscriber details between server and client,
 * mainly for subscriber management and display in tables.
 * </p>
 */
public class SubscriberDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Unique subscriber identifier (used as Subscriber ID). */
    private String id; // We might use this for the "Subscriber ID" column
    /** Subscriber's full name. */
    private String fullName;
    /** Subscriber's phone number. */
    private String phone;
    /** Subscriber's email address. */
    private String email;

    /** Subscriber's birth date (format: YYYY-MM-DD). */
    private String birthDate;
    /**
     * No-argument constructor required for serialization.
     */
    public SubscriberDTO() {}

    /**
     * Constructs a SubscriberDTO with full subscriber details.
     *
     * @param id subscriber identifier
     * @param fullName subscriber full name
     * @param phone subscriber phone number
     * @param email subscriber email address
     * @param birthDate subscriber birth date (YYYY-MM-DD)
     */
    public SubscriberDTO(String id, String fullName, String phone, String email, String birthDate) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
    }

    // Getters and Setters
    /**
     * Returns the subscriber ID.
     *
     * @return subscriber ID
     */
    public String getId() { return id; }
    /**
     * Sets the subscriber ID.
     *
     * @param id subscriber identifier
     */
    public void setId(String id) { this.id = id; }
    

    /**
     * Returns the subscriber's full name.
     *
     * @return full name
     */
    public String getFullName() { return fullName; }

    /**
     * Sets the subscriber's full name.
     *
     * @param fullName subscriber full name
     */
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    /**
     * Returns the subscriber's phone number.
     *
     * @return phone number
     */
    public String getPhone() { return phone; }
    /**
     * Sets the subscriber's phone number.
     *
     * @param phone subscriber phone number
     */
    public void setPhone(String phone) { this.phone = phone; }
    
    /**
     * Returns the subscriber's email address.
     *
     * @return email address
     */
    public String getEmail() { return email; }
    /**
     * Sets the subscriber's email address.
     *
     * @param email subscriber email
     */
    public void setEmail(String email) { this.email = email; }
    
    /**
     * Returns the subscriber's birth date.
     *
     * @return birth date (YYYY-MM-DD)
     */
    public String getBirthDate() { return birthDate; }

    /**
     * Sets the subscriber's birth date.
     *
     * @param birthDate birth date (YYYY-MM-DD)
     */
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
}