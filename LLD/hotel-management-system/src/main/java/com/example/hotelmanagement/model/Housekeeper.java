package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.AccountType;
import java.time.Duration;

public class Housekeeper extends Person {
    public Housekeeper(String name, Address address, String email, String phone, Account account) {
        super(name, address, email, phone, account);
        if (account.getAccountType() != AccountType.HOUSEKEEPER) {
            throw new IllegalArgumentException("Housekeeper must have HOUSEKEEPER account type");
        }
    }

    public RoomHouseKeeping addHouseKeeping(Room room, String description, Duration duration) {
        RoomHouseKeeping task = new RoomHouseKeeping(description, duration, this);
        task.addHouseKeeping(room);
        return task;
    }
}
