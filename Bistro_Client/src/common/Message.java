package common;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        GET_RESERVATIONS,     // client → server
        RESERVATIONS_TEXT,    // server → client
        UPDATE_RESERVATION,   // client → server
        UPDATE_RESULT,        // server → client
        INFO,                 
        ERROR                 
    }

    private Type type;

    // generic text payload (for reservations list, errors, etc.)
    private String text;

    // fields for UPDATE_RESERVATION
    private Integer reservationNumber;
    private String reservationDate;   
    private Integer numberOfGuests;

    // optional: success flag for UPDATE_RESULT
    private Boolean success;
    /**
     * Creates an empty message.
     */
    public Message() {
    }
    /**
     * Creates a message with the given type.
     */
    public Message(Type type) {
        this.type = type;
    }
    /**
     * Creates a message with the given type and text content.
     */
    public Message(Type type, String text) {
        this.type = type;
        this.text = text;
    }

  
    /**
     * Returns the message type.
     */
    public Type getType() {
        return type;
    }
    /**
     * Sets the message type.
     */
    public void setType(Type type) {
        this.type = type;
    }
    /**
     * Returns the message text.
     */
    public String getText() {
        return text;
    }
    /**
     * Sets the message text.
     */
    public void setText(String text) {
        this.text = text;
    }
    /**
     * Returns the reservation number.
     */
    public Integer getReservationNumber() {
        return reservationNumber;
    }
    /**
     * Sets the reservation number.
     */
    public void setReservationNumber(Integer reservationNumber) {
        this.reservationNumber = reservationNumber;
    }

    /**
     * Returns the reservation date.
     */
    public String getReservationDate() {
        return reservationDate;
    }
    /**
     * Sets the reservation date.
     */
    public void setReservationDate(String reservationDate) {
        this.reservationDate = reservationDate;
    }

    /**
     * Returns the number of guests.
     */
    public Integer getNumberOfGuests() {
        return numberOfGuests;
    }
    /**
     * Sets the number of guests.
     */
    public void setNumberOfGuests(Integer numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }
    /**
     * Returns whether the operation was successful.
     */
    public Boolean getSuccess() {
        return success;
    }
    
    /**
     * Sets the success status of the message.
     */
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    /**
     * Returns a string representation of the message.
     */
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", reservationId=" + reservationNumber +
                ", newDate=" + reservationDate +
                ", guests=" + numberOfGuests +
                '}';
    }

}

