package common.dto;

import java.io.Serializable;
/**
 * Data Transfer Object (DTO) representing a restaurant table.
 * <p>
 * Used to transfer table information between server and client,
 * mainly for table management and current table status display.
 * </p>
 */
public class RestaurantTableDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Unique identifier of the table (e.g., "T1", "A12"). */
    private String tableId;
    /** Number of seats available at the table. */
    private int seats;
    /** Current status of the table (e.g., "FREE", "OCCUPIED"). */
    private String status;

    /**
     * No-argument constructor required for serialization.
     */
    public RestaurantTableDTO() {}
    /**
     * Constructs a RestaurantTableDTO with all table details.
     *
     * @param tableId unique table identifier
     * @param seats number of seats at the table
     * @param status current table status
     */
    public RestaurantTableDTO(String tableId, int seats, String status) {
        this.tableId = tableId;
        this.seats = seats;
        this.status = status;
    }
    /**
     * Returns the table identifier.
     *
     * @return table ID
     */
    public String getTableId() { return tableId; }
    /**
     * Sets the table identifier.
     *
     * @param tableId unique table ID
     */
    public void setTableId(String tableId) { this.tableId = tableId; }

    /**
     * Returns the number of seats.
     *
     * @return number of seats
     */
    public int getSeats() { return seats; }
    /**
     * Sets the number of seats for the table.
     *
     * @param seats number of seats
     */
    public void setSeats(int seats) { this.seats = seats; }

    /**
     * Returns the current table status.
     *
     * @return table status
     */
    public String getStatus() { return status; }
    /**
     * Sets the current table status.
     *
     * @param status table status (e.g., FREE, OCCUPIED)
     */
    public void setStatus(String status) { this.status = status; }
    /**
     * Returns a human-readable representation of the table.
     *
     * @return formatted table description
     */
    @Override
    public String toString() {
        return tableId + " (" + seats + " seats) - " + status;
    }
}