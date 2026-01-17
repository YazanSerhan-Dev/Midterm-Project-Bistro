package common.dto;

import java.io.Serializable;

/**
 * DTO representing a login request sent from client to server.
 *
 * <p>
 * Contains the credentials required for authentication.
 * Used for both subscriber and staff login requests.
 * </p>
 */
public class LoginRequestDTO implements Serializable {
    /** Username provided by the user */
    private String username;
    /** Plain-text password provided by the user */
    private String password;
    /** Required no-args constructor for serialization */
    public LoginRequestDTO() {}
    /**
     * Constructs a login request with credentials.
     *
     * @param username user's username
     * @param password user's password
     */
    public LoginRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
    /** @return the username */
    public String getUsername() {
        return username;
    }
    /** @return the password */
    public String getPassword() {
        return password;
    }
}

