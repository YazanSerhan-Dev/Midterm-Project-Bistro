package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

public class MakeReservationResponseDTO implements Serializable {
    private boolean ok;
    private int reservationId;
    private String confirmationCode;
    private String message;

    // ✅ NEW
    private List<Timestamp> suggestedTimes;

    public MakeReservationResponseDTO() {}

    public MakeReservationResponseDTO(
            boolean ok,
            int reservationId,
            String confirmationCode,
            String message
    ) {
        this.ok = ok;
        this.reservationId = reservationId;
        this.confirmationCode = confirmationCode;
        this.message = message;
    }

    // ✅ NEW constructor (for suggestions)
    public MakeReservationResponseDTO(
            boolean ok,
            String message,
            List<Timestamp> suggestedTimes
    ) {
        this.ok = ok;
        this.reservationId = -1;
        this.confirmationCode = null;
        this.message = message;
        this.suggestedTimes = suggestedTimes;
    }

    public boolean isOk() { return ok; }
    public int getReservationId() { return reservationId; }
    public String getConfirmationCode() { return confirmationCode; }
    public String getMessage() { return message; }

    // ✅ NEW
    public List<Timestamp> getSuggestedTimes() {
        return suggestedTimes;
    }
}

