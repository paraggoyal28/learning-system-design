package com.example.hotelmanagement.model;

import java.math.BigDecimal;
import java.util.Objects;

public class KitchenService extends RoomCharge {
    private final String description;
    private final BigDecimal amount;

    public KitchenService(String description, BigDecimal amount) {
        this.description = Objects.requireNonNull(description);
        this.amount = requireNonNegative(amount);
    }

    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    @Override public InvoiceItem toInvoiceItem() { return new InvoiceItem("Kitchen: " + description, amount, 1); }
}
