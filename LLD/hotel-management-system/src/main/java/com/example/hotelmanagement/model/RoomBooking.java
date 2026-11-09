package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RoomBooking {
    public static final java.time.LocalTime STANDARD_CHECK_IN_TIME = java.time.LocalTime.of(15, 0);
    private final String reservationNumber;
    private final LocalDate startDate;
    private final int durationInDays;
    private BookingStatus status;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private final Guest guest;
    private final Room room;
    private final Invoice invoice;
    private final List<Notification> notifications = new ArrayList<>();
    private RoomKey roomKey;

    public RoomBooking(String reservationNumber, LocalDate startDate, int durationInDays,
                       BookingStatus status, Guest guest, Room room) {
        this.reservationNumber = Objects.requireNonNull(reservationNumber);
        this.startDate = Objects.requireNonNull(startDate);
        if (durationInDays < 1) throw new IllegalArgumentException("Duration must be at least one day");
        this.durationInDays = durationInDays;
        this.status = Objects.requireNonNull(status);
        this.guest = Objects.requireNonNull(guest);
        this.room = Objects.requireNonNull(room);
        this.invoice = new Invoice("INV-" + reservationNumber);
        this.invoice.addItem(new InvoiceItem("Room " + room.getRoomNumber() + " (" + durationInDays + " nights)",
                room.getBookingPrice(), durationInDays));
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return startDate.plusDays(durationInDays); }
    public int getDurationInDays() { return durationInDays; }
    public String getReservationNumber() { return reservationNumber; }
    public BookingStatus getStatus() { return status; }
    public Guest getGuest() { return guest; }
    public Room getRoom() { return room; }
    public Invoice getInvoice() { return invoice; }
    public LocalDateTime getCheckIn() { return checkIn; }
    public LocalDateTime getCheckOut() { return checkOut; }
    public RoomKey getRoomKey() { return roomKey; }
    public List<Notification> getNotifications() { return Collections.unmodifiableList(notifications); }

    public void addCharge(RoomCharge charge) { invoice.addItem(charge.toInvoiceItem()); }
    public void addNotification(Notification notification) { notifications.add(notification); }
    public void assignRoomKey(RoomKey roomKey) { this.roomKey = Objects.requireNonNull(roomKey); }

    public void checkIn() {
        if (status != BookingStatus.CONFIRMED) throw new IllegalStateException("Only confirmed bookings can check in");
        room.checkIn();
        status = BookingStatus.CHECKED_IN;
        checkIn = LocalDateTime.now();
        guest.incrementRoomsCheckedIn();
    }

    public void checkOut() {
        if (status != BookingStatus.CHECKED_IN) throw new IllegalStateException("Guest has not checked in");
        room.checkOut();
        status = BookingStatus.CHECKED_OUT;
        checkOut = LocalDateTime.now();
        if (roomKey != null) roomKey.deactivate();
    }

    public void cancel() {
        if (status != BookingStatus.CONFIRMED && status != BookingStatus.REQUESTED && status != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking cannot be cancelled from status " + status);
        }
        status = BookingStatus.CANCELLED;
    }

    /** Policy: full refund when cancellation is at least 24 hours before 3:00 PM check-in. */
    public boolean isEligibleForFullRefund(LocalDateTime cancellationTime) {
        LocalDateTime expectedCheckIn = startDate.atTime(STANDARD_CHECK_IN_TIME);
        return cancellationTime.isBefore(expectedCheckIn.minusHours(24));
    }

    public String fetchDetails() {
        return "Booking " + reservationNumber + " | " + guest.getName() + " | room " + room.getRoomNumber()
                + " | " + startDate + " to " + getEndDate() + " | " + status;
    }
}
