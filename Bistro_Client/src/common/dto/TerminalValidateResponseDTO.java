package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;
/**
 * Data Transfer Object (DTO) representing the response
 * for validating a reservation or waiting list code at the terminal.
 * <p>
 * Used both for validation and for check-in responses.
 * </p>
 */
public class TerminalValidateResponseDTO implements Serializable {

	  /** Indicates whether the provided code is valid. */
    private boolean valid;

    /** Informational or error message returned from the server. */
    private String message;

    /** Reservation identifier (greater than 0 if this is a reservation). */
    private int reservationId;

    /** Reservation date and time. */
    private Timestamp reservationTime;

    /** Number of customers associated with the reservation. */
    private int numOfCustomers;

    /** Current reservation or waiting status. */
    private String status;

    /** Indicates whether check-in is currently allowed. */
    private boolean checkInAllowed;

    // Option A: filled only after check-in
    /**
     * Table identifier assigned after successful check-in.
     * <p>
     * May be {@code null} if no table is assigned yet.
     * </p>
     */
    private String tableId;

    /**
     * No-argument constructor required for serialization.
     */
    public TerminalValidateResponseDTO() {}

    /**
     * Constructs a basic validation response.
     *
     * @param valid whether the code is valid
     * @param message response message
     */
    public TerminalValidateResponseDTO(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }
    /** @return {@code true} if the code is valid */
    public boolean isValid() { return valid; }
    /** Sets validation result. */
    public void setValid(boolean valid) { this.valid = valid; }
    

    /** @return response message */
    public String getMessage() { return message; }
    /** Sets response message. */
    public void setMessage(String message) { this.message = message; }

    /** @return reservation ID (0 if not applicable) */
    public int getReservationId() { return reservationId; }
    /** Sets reservation ID. */
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    
    /** @return reservation timestamp */
    public Timestamp getReservationTime() { return reservationTime; }
    /** Sets reservation time. */
    public void setReservationTime(Timestamp reservationTime) { this.reservationTime = reservationTime; }
    
    /** @return number of customers */
    public int getNumOfCustomers() { return numOfCustomers; }
    /** Sets number of customers. */
    public void setNumOfCustomers(int numOfCustomers) { this.numOfCustomers = numOfCustomers; }

    /** @return current status */
    public String getStatus() { return status; }
    /** Sets current status. */
    public void setStatus(String status) { this.status = status; }
    
    /** @return {@code true} if check-in is allowed */
    public boolean isCheckInAllowed() { return checkInAllowed; }
    /** Sets check-in permission flag. */
    public void setCheckInAllowed(boolean checkInAllowed) { this.checkInAllowed = checkInAllowed; }
    
    /** @return assigned table ID or {@code null} */
    public String getTableId() { return tableId; }
    /** Sets assigned table ID. */
    public void setTableId(String tableId) { this.tableId = tableId; }
}

