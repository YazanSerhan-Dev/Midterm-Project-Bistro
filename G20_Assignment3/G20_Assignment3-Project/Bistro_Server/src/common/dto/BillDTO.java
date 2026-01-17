package common.dto;

import java.io.Serializable;

/**
 * DTO representing a customer bill.
 *
 * <p>
 * Used for bill lookup and payment operations.
 * Contains pricing details, discount information and due date.
 * This class is a data container only and includes no business logic.
 * </p>
 */
public class BillDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Reservation or order confirmation code */
    private String confirmationCode;
    /** Customer full name */
    private String customerName;

    /** Number of items included in the bill */
    private int itemsCount;
    /** Total price before discounts */
    private double subtotal;
    /** Discount amount applied to the bill */
    private double discount;
    /** Final total price after discounts */
    private double total;
    /** Bill due date in YYYY-MM-DD format */
    private String dueDate; 
    /** Indicates whether a subscriber discount was applied */
    private boolean subscriberDiscountApplied;
    /** Required no-args constructor for serialization */
    public BillDTO() {}
    /**
     * Constructs a fully populated BillDTO.
     *
     * @param confirmationCode reservation or order code
     * @param customerName customer name
     * @param itemsCount number of items
     * @param subtotal price before discount
     * @param discount discount amount
     * @param total final price after discount
     * @param dueDate bill due date (YYYY-MM-DD)
     * @param subscriberDiscountApplied whether subscriber discount was applied
     */
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
    /** @return confirmation code associated with the bill */
    public String getConfirmationCode() { return confirmationCode; }
    /** @param confirmationCode confirmation code to set */
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }

    /** @return customer name */
    public String getCustomerName() { return customerName; }
    /** @param customerName customer name to set */
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    /** @return number of items in the bill */
    public int getItemsCount() { return itemsCount; }
    /** @param itemsCount number of items to set */
    public void setItemsCount(int itemsCount) { this.itemsCount = itemsCount; }

    /** @return subtotal before discounts */
    public double getSubtotal() { return subtotal; }
    /** @param subtotal subtotal amount to set */
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    /** @return discount amount */
    public double getDiscount() { return discount; }
    /** @param discount discount amount to set */
    public void setDiscount(double discount) { this.discount = discount; }

    /** @return final total price */
    public double getTotal() { return total; }
    /** @param total final total price to set */
    public void setTotal(double total) { this.total = total; }

    /** @return bill due date (YYYY-MM-DD) */
    public String getDueDate() { return dueDate; }
    /** @param dueDate due date to set */
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    /** @return true if subscriber discount was applied */
    public boolean isSubscriberDiscountApplied() { return subscriberDiscountApplied; }
    /** @param subscriberDiscountApplied flag indicating subscriber discount */
    public void setSubscriberDiscountApplied(boolean subscriberDiscountApplied) {
        this.subscriberDiscountApplied = subscriberDiscountApplied;
    }
}


