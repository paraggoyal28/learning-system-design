package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.PaymentMethod;
import com.example.hotelmanagement.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Payment processing is simulated; no real card/check/cash gateway is called. */
public class Payment {
    private final String paymentId;
    private final PaymentMethod method;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;
    private PaymentStatus status;

    public Payment(String paymentId, PaymentMethod method, BigDecimal amount) {
        this.paymentId = Objects.requireNonNull(paymentId);
        this.method = Objects.requireNonNull(method);
        this.amount = Objects.requireNonNull(amount);
        if (amount.signum() <= 0) throw new IllegalArgumentException("Payment amount must be positive");
        this.createdAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }

    public void process() { status = PaymentStatus.COMPLETED; }
    public void refund() {
        if (status != PaymentStatus.COMPLETED && status != PaymentStatus.SETTLED) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }
        status = PaymentStatus.REFUNDED;
    }

    public String getPaymentId() { return paymentId; }
    public PaymentMethod getMethod() { return method; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public PaymentStatus getStatus() { return status; }
}
