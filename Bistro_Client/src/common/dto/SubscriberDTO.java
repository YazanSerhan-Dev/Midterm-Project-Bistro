package common.dto;

import java.io.Serializable;

public class SubscriberDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // We might use this for the "Subscriber ID" column
    private String fullName;
    private String phone;
    private String email;
    private String birthDate;

    public SubscriberDTO() {}

    public SubscriberDTO(String id, String fullName, String phone, String email, String birthDate) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
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
    
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
}