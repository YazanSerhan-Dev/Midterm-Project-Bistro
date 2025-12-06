package DataBase;

import java.sql.Date;

public class Reservation {

    private int reservationNumber;
    private Date reservationDate;
    private int numberOfGuests;
    private int confirmationCode;
    private int subscriberId;
    private Date dateOfPlacing;

    //Constructor
    public Reservation(int reservationNumber,
                       Date reservationDate,
                       int numberOfGuests,
                       int confirmationCode,
                       int subscriberId,
                       Date dateOfPlacing) {
        this.reservationNumber = reservationNumber;
        this.reservationDate = reservationDate;
        this.numberOfGuests = numberOfGuests;
        this.confirmationCode = confirmationCode;
        this.subscriberId = subscriberId;
        this.dateOfPlacing = dateOfPlacing;
    }
    
    //getters
    public int getReservationNumber() { return reservationNumber; }
    public Date getReservationDate() { return reservationDate; }
    public int getNumberOfGuests() { return numberOfGuests; }
    public int getConfirmationCode() { return confirmationCode; }
    public int getSubscriberId() { return subscriberId; }
    public Date getDateOfPlacing() { return dateOfPlacing; }
}



