package common.dto;

import java.io.Serializable;

public class CurrentDinersDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int tableNumber;
    private String customerName;
    private int peopleCount;
    private String checkInTime;
    private String status;

    public CurrentDinersDTO() {}

    public CurrentDinersDTO(int tableNumber, String customerName, int peopleCount, String checkInTime, String status) {
        this.tableNumber = tableNumber;
        this.customerName = customerName;
        this.peopleCount = peopleCount;
        this.checkInTime = checkInTime;
        this.status = status;
    }

    // Getters and Setters
    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getPeopleCount() { return peopleCount; }
    public void setPeopleCount(int peopleCount) { this.peopleCount = peopleCount; }

    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}