package common.dto;

import java.io.Serializable;

/**
 * DTO used to request a monthly report from the server.
 *
 * <p>
 * Contains the month and year for which the report data
 * should be generated (performance/activity reports).
 * </p>
 */
public class ReportRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Month of the requested report (1–12).
     */
    private int month;
    /**
     * Year of the requested report (e.g., 2025).
     */
    private int year;

    // ✅ ADD THIS: No-Argument Constructor (Required for serialization)
    /**
     * No-argument constructor.
     * <p>
     * Required for serialization frameworks.
     * </p>
     */
    public ReportRequestDTO() {
    }
    /**
     * Creates a report request for a specific month and year.
     *
     * @param month month number (1–12)
     * @param year  year (e.g., 2025)
     */
    public ReportRequestDTO(int month, int year) {
        this.month = month;
        this.year = year;
    }
    /** @return requested month (1–12) */
    public int getMonth() { return month; }
    /** @return requested year */
    public int getYear() { return year; }
    
    // Optional: Add setters if your serialization library needs them
    /** @param month requested month (1–12) */
    public void setMonth(int month) { this.month = month; }
    /** @param year requested year */
    public void setYear(int year) { this.year = year; }
}