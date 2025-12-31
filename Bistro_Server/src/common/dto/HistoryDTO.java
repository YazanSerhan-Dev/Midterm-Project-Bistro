package common.dto;

import java.io.Serializable;

public class HistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;
    private String time;
    private String type;     // "Reservation"/"Visit"/etc
    private String details;
    private double amount;

    public HistoryDTO() {}

    public HistoryDTO(String date, String time, String type, String details, double amount) {
        this.date = date;
        this.time = time;
        this.type = type;
        this.details = details;
        this.amount = amount;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

