package common.dto;

import java.io.Serializable;

public class LoginResponseDTO implements Serializable {

    private boolean ok;
    private String message;

    private String username;
    private String fullName;
    private String role; 
    // role = "SUBSCRIBER", "AGENT", "MANAGER"
    
    private String memberCode;

    public LoginResponseDTO() {}

    public LoginResponseDTO(boolean ok, String message, String username, String role, String memberCode) {
        this.ok = ok;
        this.message = message;
        this.username = username;
        this.role = role;
        this.memberCode = memberCode;
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

    public String getMemberCode() {
        return memberCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }
}

