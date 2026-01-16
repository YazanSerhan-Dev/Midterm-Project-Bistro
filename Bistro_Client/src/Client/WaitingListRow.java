package Client;
/**
 * A lightweight table-row model for displaying waiting list entries in JavaFX tables.
 * <p>
 * This class represents a single record shown in the waiting list table (UI layer),
 * and typically maps data coming from the server/DTO into a format that is convenient
 * for table rendering.
 * </p>
 */
public class WaitingListRow {
	 /** Internal identifier of the waiting list request (usually DB primary key). */
    private int waitingId;
    /** Number of customers in the party. */
    private int numOfCustomers;
    /** Request creation time formatted as a display string (e.g., "yyyy-MM-dd HH:mm"). */
    private String requestTime;
    /** Current waiting list status (e.g., "WAITING", "SEATED", "CANCELLED"). */
    private String status;
    /** Public confirmation code shown to the customer and used for operations (leave/cancel). */
    private String confirmationCode;
    /**
     * Constructs a new {@code WaitingListRow} instance for UI display.
     *
     * @param waitingId        unique waiting list request id
     * @param numOfCustomers   party size
     * @param requestTime      request time as a formatted string for the UI
     * @param status           current status label for display
     * @param confirmationCode confirmation code used by the customer
     */
    public WaitingListRow(int waitingId, int numOfCustomers, String requestTime, String status, String confirmationCode) {
        this.waitingId = waitingId;
        this.numOfCustomers = numOfCustomers;
        this.requestTime = requestTime;
        this.status = status;
        this.confirmationCode = confirmationCode;
    }
    /**
     * @return the waiting list request id
     */
    public int getWaitingId() { return waitingId; }
    /**
     * @return number of customers in the group
     */
    public int getNumOfCustomers() { return numOfCustomers; }
    /**
     * @return the request time formatted for the UI
     */
    public String getRequestTime() { return requestTime; }
    /**
     * @return the current waiting status label
     */
    public String getStatus() { return status; }
    /**
     * @return the confirmation code associated with this waiting list entry
     */
    public String getConfirmationCode() { return confirmationCode; }
}