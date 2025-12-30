package common.dto;

import java.io.Serializable;

public class ProfileDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String memberNumber; // not editable
    private String fullName;     // not editable
    private String phone;        // editable
    private String email;        // editable

    public ProfileDTO() {}

    public ProfileDTO(String memberNumber, String fullName, String phone, String email) {
        this.memberNumber = memberNumber;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

