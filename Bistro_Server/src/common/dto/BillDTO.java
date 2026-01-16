package common.dto;

import java.io.Serializable;

public class BillDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String confirmationCode;
    private String customerName;
    private int itemsCount;
    private double subtotal;
    private double discount;
    private double total;
    private String dueDate; // "YYYY-MM-DD"
    private boolean subscriberDiscountApplied;

    public BillDTO() {}

    public BillDTO(String confirmationCode, String customerName, int itemsCount,
                   double subtotal, double discount, double total, String dueDate,
                   boolean subscriberDiscountApplied) {
        this.confirmationCode = confirmationCode;
        this.customerName = customerName;
        this.itemsCount = itemsCount;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.dueDate = dueDate;
        this.subscriberDiscountApplied = subscriberDiscountApplied;
    }

    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getItemsCount() { return itemsCount; }
    public void setItemsCount(int itemsCount) { this.itemsCount = itemsCount; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public boolean isSubscriberDiscountApplied() { return subscriberDiscountApplied; }
    public void setSubscriberDiscountApplied(boolean subscriberDiscountApplied) {
        this.subscriberDiscountApplied = subscriberDiscountApplied;
    }
}

