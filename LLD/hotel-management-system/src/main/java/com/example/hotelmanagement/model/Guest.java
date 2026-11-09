package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.AccountType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Guest extends Person {
    private int totalRoomsCheckedIn;
    private final List<RoomBooking> bookings = new ArrayList<>();

    public Guest(String name, Address address, String email, String phone, Account account) {
        super(name, address, email, phone, account);
        if (account.getAccountType() != AccountType.GUEST && account.getAccountType() != AccountType.MEMBER) {
            throw new IllegalArgumentException("Guest must have GUEST or MEMBER account type");
        }
    }

    public List<RoomBooking> getBookings() { return Collections.unmodifiableList(bookings); }
    public int getTotalRoomsCheckedIn() { return totalRoomsCheckedIn; }
    public void addBooking(RoomBooking booking) { bookings.add(booking); }
    public void incrementRoomsCheckedIn() { totalRoomsCheckedIn++; }

    public List<RoomBooking> getBookingsForRoom(String roomNumber) {
        return bookings.stream().filter(b -> b.getRoom().getRoomNumber().equals(roomNumber)).toList();
    }
}
