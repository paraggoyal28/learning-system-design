package com.example.hotelmanagement.notification;

import com.example.hotelmanagement.model.Notification;

public class ConsoleNotificationService implements NotificationService {
    @Override
    public void send(Notification notification) {
        System.out.printf("[NOTIFICATION:%s] to=%s | %s%n",
                notification.getType(), notification.getRecipient(), notification.getMessage());
    }
}
