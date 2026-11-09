package com.example.hotelmanagement.model;

import java.math.BigDecimal;
import java.util.Objects;

public class InvoiceItem {
    private final String description;
    private final BigDecimal unitPrice;
    private final int quantity;

    public InvoiceItem(String description, BigDecimal unitPrice, int quantity) {
        this.description = Objects.requireNonNull(description);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        if (unitPrice.signum() < 0 || quantity < 1) throw new IllegalArgumentException("Invalid invoice item amount/quantity");
        this.quantity = quantity;
    }

    public String getDescription() { return description; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    @Override
    public String toString() { return description + " x" + quantity + " = " + getTotal(); }
}
