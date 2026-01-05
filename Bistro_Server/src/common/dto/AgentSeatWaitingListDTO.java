package common.dto;

import java.io.Serializable;

public class AgentSeatWaitingListDTO implements Serializable {

    private final String confirmationCode;

    public AgentSeatWaitingListDTO(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }
}
