package model.reservation;

import model.enums.PaymentStatus;

import java.util.Date;
import java.util.UUID;

public class Payment {
    private String paymentId;
    private double amount;
    private PaymentStatus status;
    private Date timestamp;

    public Payment(String paymentId, double amount) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.timestamp = new Date();
    }

    public boolean makePayment() {
        // Interaction with Payment Gateway for eg. Paypal
        this.status = PaymentStatus.COMPLETED;
        System.out.println("Payment of $" + amount + " processed successfully.");
        return true;
    }
}
