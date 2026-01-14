package common.dto;

import java.io.Serializable;

public class ProfileDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String memberNumber;
    private String fullName;
    private String phone;
    private String email;

    // ✅ NEW
    private String barcodeData;

    public ProfileDTO() {}

    public ProfileDTO(String memberNumber, String fullName, String phone, String email, String barcodeData) {
        this.memberNumber = memberNumber;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.barcodeData = barcodeData;
    }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // ✅ NEW
    public String getBarcodeData() { return barcodeData; }
    public void setBarcodeData(String barcodeData) { this.barcodeData = barcodeData; }
}

