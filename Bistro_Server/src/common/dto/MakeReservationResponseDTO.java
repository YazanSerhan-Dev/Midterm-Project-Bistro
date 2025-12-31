package common.dto;

import java.io.Serializable;

public class MakeReservationResponseDTO implements Serializable {
    private boolean ok;
    private int reservationId;       // -1 if failed
    private String confirmationCode; // null if failed
    private String message;

    public MakeReservationResponseDTO() {}

    public MakeReservationResponseDTO(boolean ok, int reservationId, String confirmationCode, String message) {
        this.ok = ok;
        this.reservationId = reservationId;
        this.confirmationCode = confirmationCode;
        this.message = message;
    }

    public boolean isOk() { return ok; }
    public int getReservationId() { return reservationId; }
    public String getConfirmationCode() { return confirmationCode; }
    public String getMessage() { return message; }
}
