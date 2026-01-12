package common.dto;

import java.io.Serializable;

public class ReportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String label;
    private Number value1;
    private Number value2;

    // ✅ ADD THIS: No-Argument Constructor
    public ReportDTO() {
    }

    public ReportDTO(String label, Number value1, Number value2) {
        this.label = label;
        this.value1 = value1;
        this.value2 = value2;
    }

    public String getLabel() { return label; }
    public Number getValue1() { return value1; }
    public Number getValue2() { return value2; }
    
    // Setters are often required by serialization libraries to fill the object after creation
    public void setLabel(String label) { this.label = label; }
    public void setValue1(Number value1) { this.value1 = value1; }
    public void setValue2(Number value2) { this.value2 = value2; }
}