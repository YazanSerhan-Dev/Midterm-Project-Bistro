package common.dto;

import java.io.Serializable;
import java.util.List;

public class HistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;
    private String time;
    private String type;     // "Reservation"/"Visit"/etc
    private String details;
    private double amount;
    
    private List<ReservationDTO> reservations;
    private List<String> visits; // Strings describing past visits (e.g., "Date: ... Bill: ...")

    public HistoryDTO() {}

    public HistoryDTO(String date, String time, String type, String details, double amount) {
        this.date = date;
        this.time = time;
        this.type = type;
        this.details = details;
        this.amount = amount;
    }
    
    public HistoryDTO(List<ReservationDTO> reservations, List<String> visits) {
        this.reservations = reservations;
        this.visits = visits;
    }
    
    public List<ReservationDTO> getReservations() { return reservations; }
    public void setReservations(List<ReservationDTO> reservations) { this.reservations = reservations; }

    public List<String> getVisits() { return visits; }
    public void setVisits(List<String> visits) { this.visits = visits; }

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

