package com.example.hotelmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RoomService extends RoomCharge {
    private final boolean chargeable;
    private final LocalDateTime requestTime;
    private final String description;
    private final BigDecimal amount;

    public RoomService(boolean chargeable, LocalDateTime requestTime, String description, BigDecimal amount) {
        this.chargeable = chargeable;
        this.requestTime = requestTime == null ? LocalDateTime.now() : requestTime;
        this.description = description;
        this.amount = chargeable ? requireNonNegative(amount) : BigDecimal.ZERO;
    }

    public boolean isChargeable() { return chargeable; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    @Override public InvoiceItem toInvoiceItem() {
        return new InvoiceItem("Room service: " + description, amount, 1);
    }
}
