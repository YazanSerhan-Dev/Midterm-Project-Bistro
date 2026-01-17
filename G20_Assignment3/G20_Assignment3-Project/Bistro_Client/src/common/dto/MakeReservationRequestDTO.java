package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DTO representing a reservation creation request sent from client to server.
 *
 * <p>
 * The request may represent either:
 * </p>
 * <ul>
 *   <li>a subscriber reservation (identified by {@code subscriberUsername})</li>
 *   <li>a guest reservation (identified by {@code guestPhone} and/or {@code guestEmail})</li>
 * </ul>
 *
 */
public class MakeReservationRequestDTO implements Serializable {

    /** Subscriber username (used only for subscriber reservations) */
    private String subscriberUsername; 
    /** Guest phone number (used only for guest reservations) */
    private String guestPhone;        

    /** Guest email address (used only for guest reservations) */
    private String guestEmail;        
    /** Number of customers for the reservation */
    private int numOfCustomers;
    /** Requested reservation date and time */
    private Timestamp reservationTime;
    
    /** Required no-args constructor for serialization */
    public MakeReservationRequestDTO() {}
    /**
     * Constructs a reservation request.
     *
     * @param subscriberUsername subscriber username (null for guests)
     * @param guestPhone         guest phone number (null for subscribers)
     * @param guestEmail         guest email address (null for subscribers)
     * @param numOfCustomers     number of customers
     * @param reservationTime   requested reservation date and time
     */
    public MakeReservationRequestDTO(String subscriberUsername, String guestPhone, String guestEmail,
                                     int numOfCustomers, Timestamp reservationTime) {
        this.subscriberUsername = subscriberUsername;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
        this.numOfCustomers = numOfCustomers;
        this.reservationTime = reservationTime;
    }
    /** @return subscriber username, or {@code null} if this is a guest reservation */
    public String getSubscriberUsername() { return subscriberUsername; }

    /** @return guest phone number, or {@code null} if subscriber */
    public String getGuestPhone() { return guestPhone; }
    /** @return guest email address, or {@code null} if subscriber */
    public String getGuestEmail() { return guestEmail; }
    /** @return number of customers */
    public int getNumOfCustomers() { return numOfCustomers; }
    /** @return requested reservation timestamp */
    public Timestamp getReservationTime() { return reservationTime; }

    /**
     * Indicates whether this reservation request belongs to a subscriber.
     *
     * @return {@code true} if subscriber username is present
     */
    public boolean isSubscriber() {
        return subscriberUsername != null && !subscriberUsername.isBlank();
    }
}