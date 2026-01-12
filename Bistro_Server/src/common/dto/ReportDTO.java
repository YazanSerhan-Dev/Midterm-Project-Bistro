package common.dto;

import java.io.Serializable;

public class ReportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date; // The X-axis (e.g., "2025-01-01")

    // For Performance Report
    private int totalLate;
    private int totalOverstay;

    // For Activity Report
    private int totalReservations;
    private int totalWaiting;

    public ReportDTO() {}

    // Getters and Setters

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getTotalLate() { return totalLate; }
    public void setTotalLate(int totalLate) { this.totalLate = totalLate; }

    public int getTotalOverstay() { return totalOverstay; }
    public void setTotalOverstay(int totalOverstay) { this.totalOverstay = totalOverstay; }

    public int getTotalReservations() { return totalReservations; }
    public void setTotalReservations(int totalReservations) { this.totalReservations = totalReservations; }

    public int getTotalWaiting() { return totalWaiting; }
    public void setTotalWaiting(int totalWaiting) { this.totalWaiting = totalWaiting; }
}