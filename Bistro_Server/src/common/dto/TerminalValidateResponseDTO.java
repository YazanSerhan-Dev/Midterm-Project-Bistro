package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;

public class TerminalValidateResponseDTO implements Serializable {

    private boolean valid;
    private String message;

    private int reservationId;
    private Timestamp reservationTime;
    private int numOfCustomers;
    private String status;

    private boolean checkInAllowed;

    // Option A: filled only after check-in
    private String tableId;

    public TerminalValidateResponseDTO() {}

    public TerminalValidateResponseDTO(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public Timestamp getReservationTime() { return reservationTime; }
    public void setReservationTime(Timestamp reservationTime) { this.reservationTime = reservationTime; }

    public int getNumOfCustomers() { return numOfCustomers; }
    public void setNumOfCustomers(int numOfCustomers) { this.numOfCustomers = numOfCustomers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCheckInAllowed() { return checkInAllowed; }
    public void setCheckInAllowed(boolean checkInAllowed) { this.checkInAllowed = checkInAllowed; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
}

