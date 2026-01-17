package common.dto;

import java.io.Serializable;

/**
 * DTO representing restaurant opening hours.
 *
 * <p>
 * Supports both:
 * <ul>
 *   <li><b>Regular weekly hours</b> (by day of week)</li>
 *   <li><b>Special date exceptions</b> (specific calendar dates)</li>
 * </ul>
 * </p>
 */
public class OpeningHoursDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Unique identifier of the opening-hours record */
    private int hoursId;
    /**
     * Day identifier:
     * <ul>
     *   <li>For regular hours – day name (e.g. "Sunday")</li>
     *   <li>For special hours – usually "Special"</li>
     * </ul>
     */
    private String dayOfWeek;   // e.g. "Sunday" or "2025-12-25" for special dates
    /** Opening time in {@code HH:mm} format */
    private String openTime;    // "09:00"
    /** Closing time in {@code HH:mm} format */
    private String closeTime;   // "22:00"
    /** Indicates whether this entry represents a special date */
    private boolean isSpecial;
    /** Specific date in {@code yyyy-MM-dd} format (used only if {@code isSpecial == true}) */
    private String specialDate; // "2025-12-25" or null

    /** Required no-args constructor for serialization */
    public OpeningHoursDTO() {}
    /**
     * Constructs a full opening-hours entry.
     *
     * @param hoursId     unique record ID
     * @param dayOfWeek   day name or "Special"
     * @param openTime   opening time ({@code HH:mm})
     * @param closeTime  closing time ({@code HH:mm})
     * @param isSpecial  {@code true} if this is a special-date entry
     * @param specialDate specific date ({@code yyyy-MM-dd}), or {@code null}
     */
    public OpeningHoursDTO(int hoursId, String dayOfWeek, String openTime, String closeTime, boolean isSpecial, String specialDate) {
        this.hoursId = hoursId;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isSpecial = isSpecial;
        this.specialDate = specialDate;
    }
    /** @return unique opening-hours record ID */
    public int getHoursId() { return hoursId; }
    /** @return day name or "Special" */
    public String getDayOfWeek() { return dayOfWeek; }

    /** @return opening time ({@code HH:mm}) */
    public String getOpenTime() { return openTime; }
    /** @return closing time ({@code HH:mm}) */
    public String getCloseTime() { return closeTime; }
    /** @return {@code true} if this represents a special date */
    public boolean isSpecial() { return isSpecial; }

    /** @return special date ({@code yyyy-MM-dd}) or {@code null} */
    public String getSpecialDate() { return specialDate; }
    /**
     * Updates the opening time.
     *
     * @param openTime new opening time ({@code HH:mm})
     */
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    /**
     * Updates the closing time.
     *
     * @param closeTime new closing time ({@code HH:mm})
     */
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    /**
     * Returns a user-friendly string representation.
     *
     * @return formatted string: {@code "Day: HH:mm - HH:mm"}
     */
    @Override
    public String toString() {
        return dayOfWeek + ": " + openTime + " - " + closeTime;
    }
}