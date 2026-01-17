package common.dto;

import java.io.Serializable;

/**
 * DTO used for reporting and statistics.
 *
 * <p>
 * Represents a single aggregated data point for reports.
 * The same DTO is reused for different report types
 * (Performance / Activity) depending on which fields are populated.
 * </p>
 */
public class ReportDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Date representing the X-axis of the report (format: YYYY-MM-DD).
     */
    private String date; 

    // For Performance Report
    /**
     * Number of late arrivals on this date.
     */
    private int totalLate;
    /**
     * Number of overstays (customers staying beyond allowed time).
     */
    private int totalOverstay;

    // For Activity Report
    /**
     * Total number of reservations made on this date.
     */
    private int totalReservations;

    /**
     * Total number of waiting-list entries on this date.
     */
    private int totalWaiting;
    /** Required no-args constructor for serialization */
    public ReportDTO() {}

    // Getters and Setters

    /** @return report date (YYYY-MM-DD) */
    public String getDate() { return date; }
    /** @param date report date (YYYY-MM-DD) */
    public void setDate(String date) { this.date = date; }
    

    /** @return number of late arrivals */
    public int getTotalLate() { return totalLate; }
    /** @param totalLate number of late arrivals */
    public void setTotalLate(int totalLate) { this.totalLate = totalLate; }

    /** @return number of overstays */
    public int getTotalOverstay() { return totalOverstay; }
    /** @param totalOverstay number of overstays */
    public void setTotalOverstay(int totalOverstay) { this.totalOverstay = totalOverstay; }
    
    /** @return number of reservations */
    public int getTotalReservations() { return totalReservations; }
    /** @param totalReservations number of reservations */
    public void setTotalReservations(int totalReservations) { this.totalReservations = totalReservations; }

    /** @return number of waiting-list entries */
    public int getTotalWaiting() { return totalWaiting; }
    /** @param totalWaiting number of waiting-list entries */
    public void setTotalWaiting(int totalWaiting) { this.totalWaiting = totalWaiting; }
}