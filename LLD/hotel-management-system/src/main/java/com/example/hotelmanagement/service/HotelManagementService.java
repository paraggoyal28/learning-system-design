package com.example.hotelmanagement.service;

import com.example.hotelmanagement.enums.BookingStatus;
import com.example.hotelmanagement.enums.NotificationType;
import com.example.hotelmanagement.enums.PaymentMethod;
import com.example.hotelmanagement.enums.RoomStyle;
import com.example.hotelmanagement.exception.HotelManagementException;
import com.example.hotelmanagement.model.*;
import com.example.hotelmanagement.notification.NotificationService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Application/service layer: coordinates inventory, bookings, billing and notifications. */
public class HotelManagementService {
    private final Hotel hotel;
    private final NotificationService notificationService;
    private final Map<String, RoomBooking> bookings = new ConcurrentHashMap<>();
    private final AtomicLong reservationSequence = new AtomicLong(1000);
    private final AtomicLong paymentSequence = new AtomicLong(5000);
    private final AtomicLong notificationSequence = new AtomicLong(1);

    public HotelManagementService(Hotel hotel, NotificationService notificationService) {
        this.hotel = hotel;
        this.notificationService = notificationService;
    }

    public List<Room> searchRooms(String locationName, RoomStyle style, LocalDate checkIn, int durationInDays) {
        return findLocation(locationName).search(style, checkIn, durationInDays);
    }

    public RoomBooking bookRoom(Guest guest, String locationName, RoomStyle style,
                                LocalDate checkIn, int durationInDays) {
        if (!guest.getAccount().isActive()) throw new HotelManagementException("Guest account is not active");
        HotelLocation location = findLocation(locationName);
        List<Room> availableRooms = location.search(style, checkIn, durationInDays);
        if (availableRooms.isEmpty()) throw new HotelManagementException("No matching room is available for the requested dates");

        Room room = availableRooms.get(0);
        String reservationNumber = "RSV-" + reservationSequence.incrementAndGet();
        RoomBooking booking = new RoomBooking(reservationNumber, checkIn, durationInDays,
                BookingStatus.CONFIRMED, guest, room);
        location.addBooking(booking);
        bookings.put(reservationNumber, booking);
        guest.addBooking(booking);
        sendNotification(booking, NotificationType.BOOKING_CONFIRMED,
                "Your booking " + reservationNumber + " is confirmed for room " + room.getRoomNumber() + ".");
        return booking;
    }

    public List<RoomBooking> getBookingsForRoom(String locationName, String roomNumber) {
        return findLocation(locationName).getBookingsForRoom(roomNumber);
    }

    public List<RoomBooking> getBookingsForGuest(Guest guest) {
        return guest.getBookings();
    }

    public void addRoomCharge(String reservationNumber, RoomCharge charge) {
        getBooking(reservationNumber).addCharge(charge);
    }

    public Payment payInvoice(String reservationNumber, PaymentMethod method) {
        RoomBooking booking = getBooking(reservationNumber);
        BigDecimal amountDue = booking.getInvoice().getTotalAmount()
                .subtract(booking.getInvoice().getSuccessfulPaymentAmount());
        if (amountDue.signum() <= 0) throw new HotelManagementException("No amount is due for this invoice");
        Payment payment = new Payment("PAY-" + paymentSequence.incrementAndGet(), method, amountDue);
        payment.process(); // Simulates a successful cash/check/card payment.
        booking.getInvoice().addPayment(payment);
        return payment;
    }

    public RoomKey checkIn(String reservationNumber) {
        RoomBooking booking = getBooking(reservationNumber);
        booking.checkIn();
        RoomKey key = new RoomKey("KEY-" + reservationNumber, "BC-" + reservationNumber, true, false);
        key.assignRoom(booking.getRoom());
        booking.assignRoomKey(key);
        sendNotification(booking, NotificationType.CHECK_IN,
                "Check-in completed for reservation " + reservationNumber + ".");
        return key;
    }

    public Invoice checkOut(String reservationNumber) {
        RoomBooking booking = getBooking(reservationNumber);
        booking.checkOut();
        sendNotification(booking, NotificationType.CHECK_OUT,
                "Check-out completed for reservation " + reservationNumber + ".");
        return booking.getInvoice();
    }

    /** Returns the refunded amount. The 24-hour rule uses a 3:00 PM standard check-in time. */
    public BigDecimal cancelBooking(String reservationNumber, LocalDateTime cancellationTime) {
        RoomBooking booking = getBooking(reservationNumber);
        boolean refundEligible = booking.isEligibleForFullRefund(cancellationTime);
        booking.cancel();
        BigDecimal refunded = refundEligible
                ? booking.getInvoice().refundSuccessfulPayments() : BigDecimal.ZERO;
        sendNotification(booking, NotificationType.BOOKING_CANCELLED,
                "Booking " + reservationNumber + " was cancelled. Refund: " + refunded + ".");
        return refunded;
    }

    public void sendCheckInReminder(String reservationNumber) {
        RoomBooking booking = getBooking(reservationNumber);
        sendNotification(booking, NotificationType.CHECK_IN_REMINDER,
                "Reminder: check-in is scheduled for " + booking.getStartDate() + ".");
    }

    public void sendCheckOutReminder(String reservationNumber) {
        RoomBooking booking = getBooking(reservationNumber);
        sendNotification(booking, NotificationType.CHECK_OUT_REMINDER,
                "Reminder: check-out is scheduled for " + booking.getEndDate() + ".");
    }

    public RoomHouseKeeping createHousekeepingTask(String roomNumber, String locationName,
                                                    String description, Duration duration,
                                                    Housekeeper housekeeper) {
        Room room = findLocation(locationName).findRoom(roomNumber)
                .orElseThrow(() -> new HotelManagementException("Room not found: " + roomNumber));
        return housekeeper.addHouseKeeping(room, description, duration);
    }

    public void completeHousekeepingTask(String roomNumber, String locationName, RoomHouseKeeping task) {
        Room room = findLocation(locationName).findRoom(roomNumber)
                .orElseThrow(() -> new HotelManagementException("Room not found: " + roomNumber));
        task.complete(room);
    }

    public RoomBooking getBooking(String reservationNumber) {
        RoomBooking booking = bookings.get(reservationNumber);
        if (booking == null) throw new HotelManagementException("Booking not found: " + reservationNumber);
        return booking;
    }

    private HotelLocation findLocation(String locationName) {
        return hotel.getLocation(locationName)
                .orElseThrow(() -> new HotelManagementException("Hotel location not found: " + locationName));
    }

    private void sendNotification(RoomBooking booking, NotificationType type, String message) {
        Notification notification = new Notification("NTF-" + notificationSequence.getAndIncrement(),
                booking.getGuest().getEmail(), message, type);
        booking.addNotification(notification);
        notificationService.send(notification);
    }
}
