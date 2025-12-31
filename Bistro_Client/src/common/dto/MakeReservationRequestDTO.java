package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;


public class MakeReservationRequestDTO implements Serializable {
    private String subscriberUsername; // if subscriber
    private String guestPhone;         // if guest
    private String guestEmail;         // if guest

    private int numOfCustomers;
    private Timestamp reservationTime;
    

    public MakeReservationRequestDTO() {}

    public MakeReservationRequestDTO(String subscriberUsername, String guestPhone, String guestEmail,
                                     int numOfCustomers, Timestamp reservationTime) {
        this.subscriberUsername = subscriberUsername;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
        this.numOfCustomers = numOfCustomers;
        this.reservationTime = reservationTime;
    }

    public String getSubscriberUsername() { return subscriberUsername; }
    public String getGuestPhone() { return guestPhone; }
    public String getGuestEmail() { return guestEmail; }
    public int getNumOfCustomers() { return numOfCustomers; }
    public Timestamp getReservationTime() { return reservationTime; }

    public boolean isSubscriber() {
        return subscriberUsername != null && !subscriberUsername.isBlank();
    }
}