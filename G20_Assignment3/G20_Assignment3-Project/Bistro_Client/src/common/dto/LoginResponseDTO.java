package common.dto;

import java.io.Serializable;

/**
 * DTO representing a login response sent from server to client.
 *
 * <p>
 * Contains authentication result, user identity information,
 * and role-based authorization data.
 * </p>
 */
public class LoginResponseDTO implements Serializable {
	/** Indicates whether login was successful */
    private boolean ok;
    /** Optional success or error message */
    private String message;

    /** Authenticated username */
    private String username;

    /** Full name of the user (if available) */
    private String fullName;
    /**
     * User role in the system.
     * Possible values: {@code SUBSCRIBER}, {@code AGENT}, {@code MANAGER}
     */
    private String role; 
    // role = "SUBSCRIBER", "AGENT", "MANAGER"
    
    /** Subscriber member code (only for subscribers, null otherwise) */
    private String memberCode;
    /** Required no-args constructor for serialization */
    public LoginResponseDTO() {}

    /**
     * Constructs a login response.
     *
     * @param ok         whether authentication succeeded
     * @param message    response message (error or info)
     * @param username   authenticated username
     * @param role       user role in the system
     * @param memberCode subscriber member code (if applicable)
     */
    public LoginResponseDTO(boolean ok, String message, String username, String role, String memberCode) {
        this.ok = ok;
        this.message = message;
        this.username = username;
        this.role = role;
        this.memberCode = memberCode;
    }


    /** @return {@code true} if login succeeded */
    public boolean isOk() {
        return ok;
    }


    /** @return response message (error or info) */
    public String getMessage() {
        return message;
    }
    /** @return authenticated username */
    public String getUsername() {
        return username;
    }

    /** @return subscriber member code, or {@code null} if not a subscriber */
    public String getMemberCode() {
        return memberCode;
    }

    /** @return user's full name */
    public String getFullName() {
        return fullName;
    }

    /** @return user's role */
    public String getRole() {
        return role;
    }
}

