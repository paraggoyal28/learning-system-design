package com.example.hotelmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class RoomCharge {
    private final LocalDateTime issuedAt;

    protected RoomCharge() { this.issuedAt = LocalDateTime.now(); }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public abstract InvoiceItem toInvoiceItem();

    protected static BigDecimal requireNonNegative(BigDecimal amount) {
        Objects.requireNonNull(amount);
        if (amount.signum() < 0) throw new IllegalArgumentException("Charge cannot be negative");
        return amount;
    }
}
