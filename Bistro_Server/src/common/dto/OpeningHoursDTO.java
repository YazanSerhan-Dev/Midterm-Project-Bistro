package common.dto;

import java.io.Serializable;

public class OpeningHoursDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int hoursId;
    private String dayOfWeek;   // e.g. "Sunday" or "2025-12-25" for special dates
    private String openTime;    // "09:00"
    private String closeTime;   // "22:00"
    private boolean isSpecial;
    private String specialDate; // "2025-12-25" or null

    public OpeningHoursDTO() {}

    public OpeningHoursDTO(int hoursId, String dayOfWeek, String openTime, String closeTime, boolean isSpecial, String specialDate) {
        this.hoursId = hoursId;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isSpecial = isSpecial;
        this.specialDate = specialDate;
    }

    public int getHoursId() { return hoursId; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getOpenTime() { return openTime; }
    public String getCloseTime() { return closeTime; }
    public boolean isSpecial() { return isSpecial; }
    public String getSpecialDate() { return specialDate; }

    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    
    @Override
    public String toString() {
        return dayOfWeek + ": " + openTime + " - " + closeTime;
    }
}