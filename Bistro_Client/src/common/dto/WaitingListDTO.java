package common.dto;

import java.io.Serializable;

public class WaitingListDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id; 
    private String name;        // <--- Added
    private String phone;       // <--- Added
    private String email;       // <--- Added (Optional, per requirements)
    private int peopleCount;
    private String status;
    private String confirmationCode;

    public WaitingListDTO() {}

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

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getPeopleCount() { return peopleCount; }
    public void setPeopleCount(int peopleCount) { this.peopleCount = peopleCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String code) { this.confirmationCode = code; }
}