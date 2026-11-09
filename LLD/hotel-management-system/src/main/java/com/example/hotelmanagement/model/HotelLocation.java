package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.RoomStyle;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HotelLocation implements Search {
    private final String name;
    private final Address address;
    private final List<Room> rooms = new ArrayList<>();
    private final List<RoomBooking> bookings = new ArrayList<>();

    public HotelLocation(String name, Address address) {
        this.name = Objects.requireNonNull(name);
        this.address = Objects.requireNonNull(address);
    }

    public void addRoom(Room room) {
        if (rooms.stream().anyMatch(r -> r.getRoomNumber().equals(room.getRoomNumber()))) {
            throw new IllegalArgumentException("Duplicate room number: " + room.getRoomNumber());
        }
        rooms.add(Objects.requireNonNull(room));
    }

    public void addBooking(RoomBooking booking) { bookings.add(Objects.requireNonNull(booking)); }
    public List<Room> getRooms() { return Collections.unmodifiableList(rooms); }
    public List<RoomBooking> getBookings() { return Collections.unmodifiableList(bookings); }
    public String getName() { return name; }
    public Address getAddress() { return address; }
    public Optional<Room> findRoom(String roomNumber) {
        return rooms.stream().filter(r -> r.getRoomNumber().equals(roomNumber)).findFirst();
    }

    public List<RoomBooking> getBookingsForRoom(String roomNumber) {
        return bookings.stream().filter(b -> b.getRoom().getRoomNumber().equals(roomNumber)).toList();
    }

    public List<RoomBooking> getBookingsForGuest(String guestAccountId) {
        return bookings.stream().filter(b -> b.getGuest().getAccount().getId().equals(guestAccountId)).toList();
    }

    @Override
    public List<Room> search(RoomStyle style, LocalDate startDate, int durationInDays) {
        if (durationInDays < 1) throw new IllegalArgumentException("Duration must be at least one day");
        LocalDate endDate = startDate.plusDays(durationInDays);
        return rooms.stream()
                .filter(room -> style == null || room.getStyle() == style)
                .filter(room -> room.isRoomAvailable(startDate, endDate, bookings))
                .toList();
    }
}
