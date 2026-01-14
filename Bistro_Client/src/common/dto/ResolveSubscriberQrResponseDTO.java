package common.dto;

import java.io.Serializable;

public class ResolveSubscriberQrResponseDTO implements Serializable {
    private boolean found;
    private String type;            // "RESERVATION" or "WAITING_LIST"
    private String confirmationCode;
    private String message;

    public ResolveSubscriberQrResponseDTO() {}

    public ResolveSubscriberQrResponseDTO(boolean found, String type, String confirmationCode, String message) {
        this.found = found;
        this.type = type;
        this.confirmationCode = confirmationCode;
        this.message = message;
    }

    public boolean isFound() { return found; }
    public void setFound(boolean found) { this.found = found; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

