package common.dto;

import java.io.Serializable;

public class ReportRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int month;
    private int year;

    // ✅ ADD THIS: No-Argument Constructor (Required for serialization)
    public ReportRequestDTO() {
    }

    public ReportRequestDTO(int month, int year) {
        this.month = month;
        this.year = year;
    }

    public int getMonth() { return month; }
    public int getYear() { return year; }
    
    // Optional: Add setters if your serialization library needs them
    public void setMonth(int month) { this.month = month; }
    public void setYear(int year) { this.year = year; }
}