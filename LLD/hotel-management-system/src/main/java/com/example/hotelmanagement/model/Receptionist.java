package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.AccountType;
import com.example.hotelmanagement.enums.RoomStyle;
import com.example.hotelmanagement.service.HotelManagementService;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public class Receptionist extends Person {
    public Receptionist(String name, Address address, String email, String phone, Account account) {
        super(name, address, email, phone, account);
        if (account.getAccountType() != AccountType.RECEPTIONIST) {
            throw new IllegalArgumentException("Receptionist must have RECEPTIONIST account type");
        }
    }

    public Optional<Guest> searchMember(String name, Collection<Guest> guests) {
        return guests.stream().filter(g -> g.getName().equalsIgnoreCase(name)).findFirst();
    }

    public RoomBooking createBooking(HotelManagementService service, Guest guest, String locationName,
                                     RoomStyle style, LocalDate checkIn, int durationInDays) {
        return service.bookRoom(guest, locationName, style, checkIn, durationInDays);
    }

    public RoomKey checkIn(HotelManagementService service, String reservationNumber) {
        return service.checkIn(reservationNumber);
    }

    public Invoice checkOut(HotelManagementService service, String reservationNumber) {
        return service.checkOut(reservationNumber);
    }
}
