package common.dto;

import java.io.Serializable;
/**
 * DTO used for subscriber registration.
 *
 * <p>
 * Carries all required data to create a new subscriber account,
 * including credentials, personal details and identification data.
 * </p>
 */
public class RegistrationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Login username (unique) */
    private String username;
    /** Login password (raw, validated server-side) */
    private String password;
    /** Subscriber full name */
    private String fullName;
    /** Subscriber phone number */
    private String phone;

    /** Subscriber email address */
    private String email;

    /** Unique member code assigned to the subscriber */
    private String memberCode;

    /** Encoded barcode/QR identifier */
    private String barcode;
    /** Subscriber birth date (format: YYYY-MM-DD) */
    private String birthDate; // Format: YYYY-MM-DD
    /** Required no-args constructor for serialization */
    public RegistrationDTO() {}

    /**
     * Constructs a registration DTO with full subscriber details.
     *
     * @param username   login username
     * @param password   login password
     * @param fullName   subscriber full name
     * @param phone      phone number
     * @param email      email address
     * @param memberCode unique member code
     * @param barcode    encoded barcode/QR identifier
     * @param birthDate  birth date (YYYY-MM-DD)
     */
    public RegistrationDTO(String username, String password, String fullName, 
                           String phone, String email, String memberCode, 
                           String barcode, String birthDate) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.memberCode = memberCode;
        this.barcode = barcode;
        this.birthDate = birthDate;
    }

    // Getters and Setters
    /** @return login username */
    public String getUsername() { return username; }
    /** @param username new login username */
    public void setUsername(String username) { this.username = username; }
    
    /** @return login password */
    public String getPassword() { return password; }
    /** @param password new login password */
    public void setPassword(String password) { this.password = password; }

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

    /** @return subscriber member code */
    public String getMemberCode() { return memberCode; }
    /** @param memberCode assigned member code */
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    /** @return encoded barcode/QR identifier */
    public String getBarcode() { return barcode; }
    /** @param barcode updated barcode/QR identifier */
    public void setBarcode(String barcode) { this.barcode = barcode; }

    /** @return birth date (YYYY-MM-DD) */
    public String getBirthDate() { return birthDate; }
    /** @param birthDate updated birth date (YYYY-MM-DD) */
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
}