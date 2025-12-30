package common.dto;

import java.io.Serializable;

public class ReservationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String date;   // "YYYY-MM-DD"
    private String time;   // "HH:mm"
    private int guests;
    private String status;
    private double price;

    public ReservationDTO() {}

    public ReservationDTO(String code, String date, String time, int guests, String status, double price) {
        this.code = code;
        this.date = date;
        this.time = time;
        this.guests = guests;
        this.status = status;
        this.price = price;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}

