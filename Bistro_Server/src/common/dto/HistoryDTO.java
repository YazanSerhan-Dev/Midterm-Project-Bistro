package common.dto;

import java.io.Serializable;
import java.util.List;
/**
 * DTO representing historical activity of a customer or subscriber.
 *
 * <p>
 * Used to transfer history data such as past reservations,
 * visits, and financial summaries between server and client.
 * This DTO may represent either:
 * <ul>
 *   <li>A single history entry (date/time/type)</li>
 *   <li>A grouped history response (lists of reservations and visits)</li>
 * </ul>
 * </p>
 */
public class HistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Date of the history record (e.g. YYYY-MM-DD) */
    private String date;
    /** Time of the history record (e.g. HH:mm) */
    private String time;
    /** Type of record (e.g. Reservation, Visit) */
    private String type;     // "Reservation"/"Visit"/etc

    /** Additional descriptive details */
    private String details;
    /** Monetary amount related to the record (if applicable) */
    private double amount;
    /** List of past reservations */
    private List<ReservationDTO> reservations;
    /** List of textual descriptions of past visits */
    private List<String> visits; // Strings describing past visits (e.g., "Date: ... Bill: ...")
    /** Required no-args constructor for serialization */
    public HistoryDTO() {}
    /**
     * Constructs a single history entry.
     *
     * @param date record date
     * @param time record time
     * @param type record type
     * @param details descriptive details
     * @param amount related amount
     */
    public HistoryDTO(String date, String time, String type, String details, double amount) {
        this.date = date;
        this.time = time;
        this.type = type;
        this.details = details;
        this.amount = amount;
    }
    /**
     * Constructs a grouped history response.
     *
     * @param reservations list of reservations
     * @param visits list of visit descriptions
     */
    public HistoryDTO(List<ReservationDTO> reservations, List<String> visits) {
        this.reservations = reservations;
        this.visits = visits;
    }
    
    /** @return list of reservations */
    public List<ReservationDTO> getReservations() { return reservations; }
    /** @param reservations reservations to set */
    public void setReservations(List<ReservationDTO> reservations) { this.reservations = reservations; }


    /** @return list of visit descriptions */
    public List<String> getVisits() { return visits; }
    /** @param visits visit descriptions to set */
    public void setVisits(List<String> visits) { this.visits = visits; }

    /** @return record date */
    public String getDate() { return date; }
    /** @param date record date to set */
    public void setDate(String date) { this.date = date; }

    /** @return record time */
    public String getTime() { return time; }
    /** @param time record time to set */
    public void setTime(String time) { this.time = time; }

    /** @return record type */
    public String getType() { return type; }
    /** @param type record type to set */
    public void setType(String type) { this.type = type; }


    /** @return descriptive details */
    public String getDetails() { return details; }
    /** @param details details to set */
    public void setDetails(String details) { this.details = details; }

    /** @return related monetary amount */
    public double getAmount() { return amount; }
    /** @param amount amount to set */
    public void setAmount(double amount) { this.amount = amount; }
}

