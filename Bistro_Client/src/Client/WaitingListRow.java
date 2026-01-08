package Client;

public class WaitingListRow {
    private int waitingId;
    private int numOfCustomers;
    private String requestTime;
    private String status;
    private String confirmationCode;

    public WaitingListRow(int waitingId, int numOfCustomers, String requestTime, String status, String confirmationCode) {
        this.waitingId = waitingId;
        this.numOfCustomers = numOfCustomers;
        this.requestTime = requestTime;
        this.status = status;
        this.confirmationCode = confirmationCode;
    }

    public int getWaitingId() { return waitingId; }
    public int getNumOfCustomers() { return numOfCustomers; }
    public String getRequestTime() { return requestTime; }
    public String getStatus() { return status; }
    public String getConfirmationCode() { return confirmationCode; }
}