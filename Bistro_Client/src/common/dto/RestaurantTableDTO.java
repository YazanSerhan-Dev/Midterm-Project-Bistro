package common.dto;

import java.io.Serializable;

public class RestaurantTableDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tableId;
    private int seats;
    private String status;

    public RestaurantTableDTO() {}

    public RestaurantTableDTO(String tableId, int seats, String status) {
        this.tableId = tableId;
        this.seats = seats;
        this.status = status;
    }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
    public String toString() {
        return tableId + " (" + seats + " seats) - " + status;
    }
}