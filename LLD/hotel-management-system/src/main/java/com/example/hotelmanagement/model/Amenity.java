package com.example.hotelmanagement.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Amenity extends RoomCharge {
    private final String name;
    private final String description;
    private final BigDecimal amount;

    public Amenity(String name, String description, BigDecimal amount) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.amount = requireNonNegative(amount);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    @Override public InvoiceItem toInvoiceItem() { return new InvoiceItem("Amenity: " + name + " - " + description, amount, 1); }
}
