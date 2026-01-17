package common.dto;

import java.io.Serializable;
/**
 * Data Transfer Object representing a reservation in the system.
 *
 * <p>
 * Used for transferring reservation data between server and client,
 * including reservation details, status, and timing information.
 * </p>
 */
public class ReservationDTO implements Serializable { 
	 /**
     * Unique identifier of the reservation.
     */
    private int reservationId;
    /**
     * Confirmation code given to the customer.
     */
    private String confirmationCode;

    /**
     * Reservation date and time (formatted string).
     */

    private String reservationTime;
    /**
     * Expiry time of the reservation (formatted string).
     */
    private String expiryTime;
    /**
     * Number of customers included in the reservation.
     */
    private int numOfCustomers;
    /**
     * Current reservation status (e.g., ACTIVE, CANCELLED, ARRIVED).
     */
    private String status;
    /**
     * No-argument constructor.
     * <p>
     * Required for serialization.
     * </p>
     */
    public ReservationDTO() {}

    /**
     * Full constructor for reservation details.
     *
     * @param reservationId     reservation ID
     * @param confirmationCode  confirmation code
     * @param reservationTime   reservation date and time
     * @param expiryTime        reservation expiry time
     * @param numOfCustomers    number of customers
     * @param status            reservation status
     */
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

    
    /**
     * Partial constructor used when only basic reservation data is required.
     *
     * @param reservationId   reservation ID
     * @param reservationTime reservation date and time
     * @param numOfCustomers  number of customers
     */
    public ReservationDTO(int reservationId, String reservationTime, int numOfCustomers) {
        this.reservationId = reservationId;
        this.reservationTime = reservationTime;
        this.numOfCustomers = numOfCustomers;
    }

    // Getters and Setters...

    /** @return reservation ID */
    public int getReservationId() { return reservationId; }
    /** @param reservationId reservation ID */
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    
    /** @return confirmation code */
    public String getConfirmationCode() { return confirmationCode; }
    /** @param confirmationCode confirmation code */
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }
    
    /** @return reservation date and time */
    public String getReservationTime() { return reservationTime; }
    /** @param reservationTime reservation date and time */
    public void setReservationTime(String reservationTime) { this.reservationTime = reservationTime; }

    /** @return reservation expiry time */
    public String getExpiryTime() { return expiryTime; }
    /** @param expiryTime reservation expiry time */
    public void setExpiryTime(String expiryTime) { this.expiryTime = expiryTime; }
    
    /** @return number of customers */
    public int getNumOfCustomers() { return numOfCustomers; }
    /** @param numOfCustomers number of customers */
    public void setNumOfCustomers(int numOfCustomers) { this.numOfCustomers = numOfCustomers; }
    
    /** @return reservation status */
    public String getStatus() { return status; }
    /** @param status reservation status */
    public void setStatus(String status) { this.status = status; }
}