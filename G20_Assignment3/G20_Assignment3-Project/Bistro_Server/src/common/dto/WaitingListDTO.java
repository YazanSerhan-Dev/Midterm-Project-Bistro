package common.dto;

import java.io.Serializable;

/**
 * DTO representing a waiting list entry for walk-in customers or subscribers.
 */
public class WaitingListDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id; 
    private String name;        // <--- Added
    private String phone;       // <--- Added
    private String email;       // <--- Added (Optional, per requirements)
    private int peopleCount;
    private String status;
    private String confirmationCode;
    private String requestTime;
    /** Required for serialization. */
    public WaitingListDTO() {}
    /**
     * Full constructor including contact details.
     */
    public WaitingListDTO(int id, String name, String phone, String email, 
                          int peopleCount, String status, String confirmationCode) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.peopleCount = peopleCount;
        this.status = status;
        this.confirmationCode = confirmationCode;
    }
    /**
     * Constructor for minimal waiting list representation.
     */
    public WaitingListDTO(int id, int peopleCount, String requestTime, String status, String confirmationCode) {
        this.id = id;
        this.peopleCount = peopleCount;
        this.requestTime = requestTime;
        this.status = status;
        this.confirmationCode = confirmationCode;
        // Leave optional fields null
        this.name = null;
        this.phone = null;
        this.email = null;
    }

    // Getters and Setters
    /** @return waiting list ID */
    public int getId() { return id; }
    /** Sets waiting list ID. */
    public void setId(int id) { this.id = id; }

    /** @return customer name */
    public String getName() { return name; }
    /** Sets customer name. */
    public void setName(String name) { this.name = name; }
    
    /** @return phone number */
    public String getPhone() { return phone; }
    /** Sets phone number. */
    public void setPhone(String phone) { this.phone = phone; }

    /** @return email address */
    public String getEmail() { return email; }
    /** Sets email address. */
    public void setEmail(String email) { this.email = email; }
    
    /** @return number of guests */
    public int getPeopleCount() { return peopleCount; }
    /** Sets number of guests. */
    public void setPeopleCount(int peopleCount) { this.peopleCount = peopleCount; }

    /** @return current waiting status */
    public String getStatus() { return status; }
    /** Sets waiting status. */
    public void setStatus(String status) { this.status = status; }
    
    /** @return confirmation code */
    public String getConfirmationCode() { return confirmationCode; }
    /** Sets confirmation code. */
    public void setConfirmationCode(String code) { this.confirmationCode = code; }
    /** @return request time */
    public String getRequestTime() { return requestTime; }
    /** Sets request time. */
    public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
}