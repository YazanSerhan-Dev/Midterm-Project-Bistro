package common.dto;

import java.io.Serializable;

public class WaitingListDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int waitingId;
    private int numOfCustomers;
    private String requestTime;
    private String status;
    private String confirmationCode;
    
    public WaitingListDTO() {}

    public WaitingListDTO(int waitingId, int numOfCustomers, String requestTime, String status, String confirmationCode) {
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

    public void setWaitingId(int waitingId) { this.waitingId = waitingId; }
    public void setNumOfCustomers(int numOfCustomers) { this.numOfCustomers = numOfCustomers; }
    public void setRequestTime(String requestTime) { this.requestTime = requestTime; }
    public void setStatus(String status) { this.status = status; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }
}