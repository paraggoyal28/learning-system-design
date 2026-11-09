package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.BookingStatus;
import com.example.hotelmanagement.enums.RoomStatus;
import com.example.hotelmanagement.enums.RoomStyle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Room {
    private final String roomNumber;
    private final RoomStyle style;
    private RoomStatus status;
    private final BigDecimal bookingPrice;
    private final boolean smoking;
    private final List<RoomKey> keys = new ArrayList<>();
    private final List<RoomHouseKeeping> houseKeepingLog = new ArrayList<>();

    public Room(String roomNumber, RoomStyle style, RoomStatus status, BigDecimal bookingPrice, boolean smoking) {
        this.roomNumber = Objects.requireNonNull(roomNumber);
        this.style = Objects.requireNonNull(style);
        this.status = Objects.requireNonNull(status);
        this.bookingPrice = Objects.requireNonNull(bookingPrice);
        if (bookingPrice.signum() < 0) throw new IllegalArgumentException("Room price cannot be negative");
        this.smoking = smoking;
    }

    public String getRoomNumber() { return roomNumber; }
    public RoomStyle getStyle() { return style; }
    public RoomStatus getStatus() { return status; }
    public BigDecimal getBookingPrice() { return bookingPrice; }
    public boolean isSmoking() { return smoking; }
    public List<RoomKey> getKeys() { return Collections.unmodifiableList(keys); }
    public List<RoomHouseKeeping> getHouseKeepingLog() { return Collections.unmodifiableList(houseKeepingLog); }

    public boolean isRoomAvailable() { return status == RoomStatus.AVAILABLE; }

    public boolean isRoomAvailable(LocalDate requestedStart, LocalDate requestedEnd,
                                   Collection<RoomBooking> bookings) {
        if (status != RoomStatus.AVAILABLE) return false;
        return bookings.stream()
                .filter(b -> b.getRoom().equals(this))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.CHECKED_IN)
                .noneMatch(b -> requestedStart.isBefore(b.getEndDate()) && b.getStartDate().isBefore(requestedEnd));
    }

    public void checkIn() {
        if (status != RoomStatus.AVAILABLE) {
            throw new IllegalStateException("Room " + roomNumber + " is not operationally available");
        }
        status = RoomStatus.OCCUPIED;
    }

    public void checkOut() {
        if (status != RoomStatus.OCCUPIED) {
            throw new IllegalStateException("Room " + roomNumber + " is not occupied");
        }
        status = RoomStatus.AVAILABLE;
    }

    public void changeStatus(RoomStatus newStatus) { this.status = Objects.requireNonNull(newStatus); }
    public void addKey(RoomKey key) { keys.add(Objects.requireNonNull(key)); }
    public void addHouseKeepingEntry(RoomHouseKeeping entry) { houseKeepingLog.add(Objects.requireNonNull(entry)); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Room room)) return false;
        return roomNumber.equals(room.roomNumber);
    }

    @Override
    public int hashCode() { return roomNumber.hashCode(); }

    @Override
    public String toString() {
        return "Room " + roomNumber + " [" + style + ", " + status + ", " + bookingPrice + " per night]";
    }
}
