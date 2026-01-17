package DataBase;

import java.sql.Timestamp;

/**
 * Entity that matches table: reservation
 *
 * Columns:
 * 
 *  - reservation_id
 *  - num_of_customers
 *  - reservation_time
 *  - expiry_time
 *  - status
 *  - confirmation_code
 */
public class Reservation {

    private int reservationId;          // reservation_id
    private int numOfCustomers;         // num_of_customers
    private Timestamp reservationTime;  // reservation_time
    private Timestamp expiryTime;       // expiry_time
    private String status;              // status
    private String confirmationCode;    // confirmation_code

    // Empty constructor (useful for frameworks / flexibility)
    public Reservation() {}

    // Full constructor
    public Reservation(int reservationId,
                       int numOfCustomers,
                       Timestamp reservationTime,
                       Timestamp expiryTime,
                       String status,
                       String confirmationCode) {
        this.reservationId = reservationId;
        this.numOfCustomers = numOfCustomers;
        this.reservationTime = reservationTime;
        this.expiryTime = expiryTime;
        this.status = status;
        this.confirmationCode = confirmationCode;
    }

    // Getters
    public int getReservationId() {
        return reservationId;
    }

    public int getNumOfCustomers() {
        return numOfCustomers;
    }

    public Timestamp getReservationTime() {
        return reservationTime;
    }

    public Timestamp getExpiryTime() {
        return expiryTime;
    }

    public String getStatus() {
        return status;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    // Setters
    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public void setNumOfCustomers(int numOfCustomers) {
        this.numOfCustomers = numOfCustomers;
    }

    public void setReservationTime(Timestamp reservationTime) {
        this.reservationTime = reservationTime;
    }

    public void setExpiryTime(Timestamp expiryTime) {
        this.expiryTime = expiryTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId=" + reservationId +
                ", numOfCustomers=" + numOfCustomers +
                ", reservationTime=" + reservationTime +
                ", expiryTime=" + expiryTime +
                ", status='" + status + '\'' +
                ", confirmationCode='" + confirmationCode + '\'' +
                '}';
    }
}



