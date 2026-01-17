package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
/**
 * DTO representing a response to a reservation creation or availability request.
 *
 * <p>
 * The response can represent:
 * <ul>
 *   <li>a successful reservation (with ID and confirmation code)</li>
 *   <li>a failed reservation (with an explanatory message)</li>
 *   <li>an availability response containing alternative suggested times</li>
 * </ul>
 * </p>
 */
public class MakeReservationResponseDTO implements Serializable {
	   /** Indicates whether the operation succeeded */
    private boolean ok;
    /** Reservation ID (valid only if {@code ok == true}) */
    private int reservationId;
    /** Reservation confirmation code (valid only if {@code ok == true}) */
    private String confirmationCode;
    /** Informational or error message returned from the server */
    private String message;


    /** Alternative suggested reservation times (used when no availability exists) */
    private List<Timestamp> suggestedTimes;

    /** Required no-args constructor for serialization */
    public MakeReservationResponseDTO() {}
    /**
     * Constructs a standard reservation response.
     *
     * @param ok               whether the reservation was successful
     * @param reservationId   generated reservation ID
     * @param confirmationCode confirmation code for the reservation
     * @param message          informational or error message
     */
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

    /**
     * Constructs a response containing alternative suggested reservation times.
     *
     * <p>
     * Used when the requested time is unavailable.
     * </p>
     *
     * @param ok             should typically be {@code false}
     * @param message        explanation message
     * @param suggestedTimes list of alternative available times
     */
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

    /** @return {@code true} if the reservation was created successfully */
    public boolean isOk() { return ok; }
    /** @return reservation ID, or {@code -1} if not created */
    public int getReservationId() { return reservationId; }
    /** @return reservation confirmation code, or {@code null} if not created */
    public String getConfirmationCode() { return confirmationCode; }
    /** @return informational or error message */
    public String getMessage() { return message; }


    /** @return list of alternative suggested reservation times (may be {@code null}) */
    public List<Timestamp> getSuggestedTimes() {
        return suggestedTimes;
    }
}
