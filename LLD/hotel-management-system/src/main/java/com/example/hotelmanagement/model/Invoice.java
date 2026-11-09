package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Invoice {
    private final String invoiceNumber;
    private final List<InvoiceItem> items = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();

    public Invoice(String invoiceNumber) { this.invoiceNumber = Objects.requireNonNull(invoiceNumber); }

    public void addItem(InvoiceItem item) {
        if (getPaymentStatus() == PaymentStatus.COMPLETED || getPaymentStatus() == PaymentStatus.SETTLED) {
            throw new IllegalStateException("Cannot add charges after the invoice has been paid");
        }
        items.add(Objects.requireNonNull(item));
    }

    public void addPayment(Payment payment) {
        payments.add(Objects.requireNonNull(payment));
    }

    public BigDecimal getTotalAmount() {
        return items.stream().map(InvoiceItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getSuccessfulPaymentAmount() {
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED || p.getStatus() == PaymentStatus.SETTLED)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal refundSuccessfulPayments() {
        BigDecimal refunded = BigDecimal.ZERO;
        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.SETTLED) {
                refunded = refunded.add(payment.getAmount());
                payment.refund();
            }
        }
        return refunded;
    }

    public PaymentStatus getPaymentStatus() {
        if (payments.isEmpty()) return PaymentStatus.UNPAID;
        if (payments.stream().anyMatch(p -> p.getStatus() == PaymentStatus.COMPLETED || p.getStatus() == PaymentStatus.SETTLED)) {
            return getSuccessfulPaymentAmount().compareTo(getTotalAmount()) >= 0
                    ? PaymentStatus.COMPLETED : PaymentStatus.PENDING;
        }
        if (payments.stream().allMatch(p -> p.getStatus() == PaymentStatus.REFUNDED)) return PaymentStatus.REFUNDED;
        return payments.get(payments.size() - 1).getStatus();
    }

    public String getInvoiceNumber() { return invoiceNumber; }
    public List<InvoiceItem> getItems() { return Collections.unmodifiableList(items); }
    public List<Payment> getPayments() { return Collections.unmodifiableList(payments); }

    public String print() {
        StringBuilder result = new StringBuilder("Invoice ").append(invoiceNumber).append(System.lineSeparator());
        items.forEach(item -> result.append("  - ").append(item).append(System.lineSeparator()));
        result.append("Total: ").append(getTotalAmount()).append(System.lineSeparator());
        result.append("Payment status: ").append(getPaymentStatus());
        return result.toString();
    }
}
