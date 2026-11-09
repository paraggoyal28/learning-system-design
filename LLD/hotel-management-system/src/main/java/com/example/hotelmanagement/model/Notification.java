package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.Objects;

public class Notification {
    private final String notificationId;
    private final String recipient;
    private final String message;
    private final NotificationType type;
    private final LocalDateTime createdAt;

    public Notification(String notificationId, String recipient, String message, NotificationType type) {
        this.notificationId = Objects.requireNonNull(notificationId);
        this.recipient = Objects.requireNonNull(recipient);
        this.message = Objects.requireNonNull(message);
        this.type = Objects.requireNonNull(type);
        this.createdAt = LocalDateTime.now();
    }

    public String getNotificationId() { return notificationId; }
    public String getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
