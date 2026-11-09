package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.AccountType;

public class Server extends Person {
    public Server(String name, Address address, String email, String phone, Account account) {
        super(name, address, email, phone, account);
        if (account.getAccountType() != AccountType.SERVER) {
            throw new IllegalArgumentException("Server must have SERVER account type");
        }
    }

    public void addRoomCharge(RoomBooking booking, RoomCharge roomCharge) {
        booking.addCharge(roomCharge);
    }
}
