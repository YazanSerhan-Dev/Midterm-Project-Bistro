package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;
/**
 * Data Transfer Object (DTO) representing an active terminal item.
 * <p>
 * Used for displaying active reservations or waiting list entries
 * associated with a subscriber at the terminal screen.
 * </p>
 */
public class TerminalActiveItemDTO implements Serializable {

    /** Item type: "RESERVATION" or "WAITING". */
    private String type;

    /** Confirmation code associated with the item. */
    private String confirmationCode;

    /** Current status of the item (e.g., ACTIVE, WAITING, ARRIVED). */
    private String status;

    /** Reservation or waiting list time. */
    private Timestamp time;

    /** Number of guests associated with the item. */
    private int peopleCount;

    /**
     * No-argument constructor required for serialization.
     */
    public TerminalActiveItemDTO() {}

    /**
     * Constructs a TerminalActiveItemDTO with full item details.
     *
     * @param type item type (RESERVATION / WAITING)
     * @param confirmationCode confirmation code
     * @param status current status
     * @param time reservation or waiting time
     * @param peopleCount number of guests
     */
    public TerminalActiveItemDTO(String type, String confirmationCode,
                                 String status, Timestamp time, int peopleCount) {
        this.type = type;
        this.confirmationCode = confirmationCode;
        this.status = status;
        this.time = time;
        this.peopleCount = peopleCount;
    }

    /**
     * Returns the item type.
     *
     * @return item type
     */
    public String getType() { return type; }
    /**
     * Returns the confirmation code.
     *
     * @return confirmation code
     */
    public String getConfirmationCode() { return confirmationCode; }
    /**
     * Returns the current item status.
     *
     * @return status
     */
    public String getStatus() { return status; }
    /**
     * Returns the reservation or waiting time.
     *
     * @return timestamp of the item
     */
    public Timestamp getTime() { return time; }

    /**
     * Returns the number of guests.
     *
     * @return people count
     */
    public int getPeopleCount() { return peopleCount; }
    /**
     * Returns a formatted string representation for UI display.
     *
     * @return formatted item summary
     */
    @Override
    public String toString() {
        String t = "-";
        try {
            if (this.time != null) {
                t = this.time.toLocalDateTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                );
            }
        } catch (Exception ignored) {}

        String ty = (this.type == null || this.type.isBlank()) ? "-" : this.type.trim();
        String st = (this.status == null || this.status.isBlank()) ? "-" : this.status.trim();
        int ppl = Math.max(this.peopleCount, 0);

        return ty + " | " + ppl + " guests | " + st + " | " + t;
    }

}
