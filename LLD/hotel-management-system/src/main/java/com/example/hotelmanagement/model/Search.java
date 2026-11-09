package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.RoomStyle;
import java.time.LocalDate;
import java.util.List;

/** Search abstraction implemented by an inventory/location, not by an individual room. */
public interface Search {
    List<Room> search(RoomStyle style, LocalDate startDate, int durationInDays);
}
