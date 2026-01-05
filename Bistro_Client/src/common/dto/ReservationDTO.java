package common.dto;

import java.io.Serializable;

public class ReservationDTO implements Serializable { 

    private int reservationId;
    private String confirmationCode;
    private String reservationTime;
    private String expiryTime;
    private int numOfCustomers;
    private String status;

    public ReservationDTO() {}

   
    public ReservationDTO(int reservationId, String confirmationCode,
                          String reservationTime, String expiryTime,
                          int numOfCustomers, String status) {
        this.reservationId = reservationId;
        this.confirmationCode = confirmationCode;
        this.reservationTime = reservationTime;
        this.expiryTime = expiryTime;
        this.numOfCustomers = numOfCustomers;
        this.status = status;
    }

    //  Optional Constructor (if your calling code creates objects with fewer fields)
    public ReservationDTO(int reservationId, String reservationTime, int numOfCustomers) {
        this.reservationId = reservationId;
        this.reservationTime = reservationTime;
        this.numOfCustomers = numOfCustomers;
    }

    // Getters and Setters...
    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }

    public String getReservationTime() { return reservationTime; }
    public void setReservationTime(String reservationTime) { this.reservationTime = reservationTime; }

    public String getExpiryTime() { return expiryTime; }
    public void setExpiryTime(String expiryTime) { this.expiryTime = expiryTime; }

    public int getNumOfCustomers() { return numOfCustomers; }
    public void setNumOfCustomers(int numOfCustomers) { this.numOfCustomers = numOfCustomers; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}