package common;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        GET_RESERVATIONS,     // client → server
        RESERVATIONS_TEXT,    // server → client
        UPDATE_RESERVATION,   // client → server
        UPDATE_RESULT,        // server → client
        INFO,                 // optional
        ERROR                 // optional
    }

    private Type type;

    // generic text payload (for reservations list, errors, etc.)
    private String text;

    // fields for UPDATE_RESERVATION
    private Integer reservationNumber;
    private String reservationDate;   // simplest: yyyy-MM-dd as String
    private Integer numberOfGuests;

    // optional: success flag for UPDATE_RESULT
    private Boolean success;

    public Message() {
    }

    public Message(Type type) {
        this.type = type;
    }

    public Message(Type type, String text) {
        this.type = type;
        this.text = text;
    }

    // getters and setters

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getReservationNumber() {
        return reservationNumber;
    }

    public void setReservationNumber(Integer reservationNumber) {
        this.reservationNumber = reservationNumber;
    }

    public String getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(String reservationDate) {
        this.reservationDate = reservationDate;
    }

    public Integer getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(Integer numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
