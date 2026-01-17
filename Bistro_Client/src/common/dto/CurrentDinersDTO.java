package common.dto;

import java.io.Serializable;

/**
 * DTO representing a currently seated group in the restaurant.
 *
 * <p>
 * Used by staff and management screens to display real-time
 * information about occupied tables and active diners.
 * This class is a pure data container with no business logic.
 * </p>
 */
public class CurrentDinersDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Table number assigned to the diners */
    private int tableNumber;
    /** Name of the customer or reservation holder */
    private String customerName;

    /** Number of people seated at the table */
    private int peopleCount;
    /** Check-in time as a formatted string */
    private String checkInTime;
    /** Current dining status (e.g. SEATED, IN_PROGRESS) */
    private String status;

    /** Required no-args constructor for serialization */
    public CurrentDinersDTO() {}
    /**
     * Constructs a fully populated CurrentDinersDTO.
     *
     * @param tableNumber assigned table number
     * @param customerName customer name
     * @param peopleCount number of diners
     * @param checkInTime check-in time
     * @param status current dining status
     */
    public CurrentDinersDTO(int tableNumber, String customerName, int peopleCount, String checkInTime, String status) {
        this.tableNumber = tableNumber;
        this.customerName = customerName;
        this.peopleCount = peopleCount;
        this.checkInTime = checkInTime;
        this.status = status;
    }

    // Getters and Setters
    /** @return table number */
    public int getTableNumber() { return tableNumber; }
    /** @param tableNumber table number to set */
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    /** @return customer name */
    public String getCustomerName() { return customerName; }
    /** @param customerName customer name to set */
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    /** @return number of people seated */
    public int getPeopleCount() { return peopleCount; }
    /** @param peopleCount number of people to set */
    public void setPeopleCount(int peopleCount) { this.peopleCount = peopleCount; }

    /** @return check-in time */
    public String getCheckInTime() { return checkInTime; }
    /** @param checkInTime check-in time to set */
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
    
    /** @return current dining status */
    public String getStatus() { return status; }
    /** @param status dining status to set */
    public void setStatus(String status) { this.status = status; }
}