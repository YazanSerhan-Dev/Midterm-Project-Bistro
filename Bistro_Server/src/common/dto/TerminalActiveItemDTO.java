package common.dto;

import java.io.Serializable;
import java.sql.Timestamp;

public class TerminalActiveItemDTO implements Serializable {

    private String type;            // RESERVATION / WAITING
    private String confirmationCode;
    private String status;
    private Timestamp time;
    private int peopleCount;

    public TerminalActiveItemDTO() {}

    public TerminalActiveItemDTO(String type, String confirmationCode,
                                 String status, Timestamp time, int peopleCount) {
        this.type = type;
        this.confirmationCode = confirmationCode;
        this.status = status;
        this.time = time;
        this.peopleCount = peopleCount;
    }

    public String getType() { return type; }
    public String getConfirmationCode() { return confirmationCode; }
    public String getStatus() { return status; }
    public Timestamp getTime() { return time; }
    public int getPeopleCount() { return peopleCount; }
    
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
