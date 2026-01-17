package common.dto;

import java.io.Serializable;
/**
 * DTO representing a subscriber profile.
 *
 * <p>
 * Used for displaying and updating subscriber personal details,
 * and for identifying the subscriber via barcode/QR at terminals.
 * </p>
 */
public class ProfileDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Unique subscriber/member number */
    private String memberNumber;
    /** Subscriber full name */
    private String fullName;
    /** Subscriber phone number */
    private String phone;
    /** Subscriber email address */
    private String email;

    /** Encoded barcode/QR data used for terminal identification */
    private String barcodeData;
    /** Required no-args constructor for serialization */
    public ProfileDTO() {}

    /**
     * Constructs a full subscriber profile DTO.
     *
     * @param memberNumber unique subscriber number
     * @param fullName     subscriber full name
     * @param phone        phone number
     * @param email        email address
     * @param barcodeData encoded barcode/QR data
     */
    public ProfileDTO(String memberNumber, String fullName, String phone, String email, String barcodeData) {
        this.memberNumber = memberNumber;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.barcodeData = barcodeData;
    }
    /** @return subscriber member number */
    public String getMemberNumber() { return memberNumber; }
    /** @param memberNumber new subscriber member number */
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
    
    /** @return subscriber full name */
    public String getFullName() { return fullName; }
    /** @param fullName updated subscriber full name */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return subscriber phone number */
    public String getPhone() { return phone; }
    /** @param phone updated phone number */
    public void setPhone(String phone) { this.phone = phone; }
 
    /** @return subscriber email address */
    public String getEmail() { return email; }
    /** @param email updated email address */
    public void setEmail(String email) { this.email = email; }

    /** @return encoded barcode/QR data */
    public String getBarcodeData() { return barcodeData; }
    /** @param barcodeData updated barcode/QR data */
    public void setBarcodeData(String barcodeData) { this.barcodeData = barcodeData; }
}

