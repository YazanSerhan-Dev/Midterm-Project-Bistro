package common.dto;

import java.io.Serializable;

public class SubscriberDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // We might use this for the "Subscriber ID" column
    private String fullName;
    private String phone;
    private String email;
    private String status; // e.g., "Active", "Frozen"

    public SubscriberDTO() {}

    public SubscriberDTO(String id, String fullName, String phone, String email, String status) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}