package common.dto;

import java.io.Serializable;

public class LoginResponseDTO implements Serializable {

    private boolean ok;
    private String message;

    private String username;
    private String fullName;
    private String role; 
    // role = "SUBSCRIBER", "AGENT", "MANAGER"

    public LoginResponseDTO() {}

    public LoginResponseDTO(
            boolean ok,
            String message,
            String username,
            String fullName,
            String role
    ) {
        this.ok = ok;
        this.message = message;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }
}

