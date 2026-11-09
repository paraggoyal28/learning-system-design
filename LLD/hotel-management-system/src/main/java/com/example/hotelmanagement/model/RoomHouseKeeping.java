package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.HousekeepingStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class RoomHouseKeeping {
    private final String description;
    private final LocalDateTime startDateTime;
    private final Duration duration;
    private final Housekeeper houseKeeper;
    private HousekeepingStatus status;

    public RoomHouseKeeping(String description, Duration duration, Housekeeper houseKeeper) {
        this.description = Objects.requireNonNull(description);
        this.duration = Objects.requireNonNull(duration);
        this.houseKeeper = Objects.requireNonNull(houseKeeper);
        this.startDateTime = LocalDateTime.now();
        this.status = HousekeepingStatus.SCHEDULED;
    }

    public void addHouseKeeping(Room room) {
        room.addHouseKeepingEntry(this);
        this.status = HousekeepingStatus.IN_PROGRESS;
        room.changeStatus(com.example.hotelmanagement.enums.RoomStatus.BEING_SERVICED);
    }

    public void complete(Room room) {
        this.status = HousekeepingStatus.COMPLETED;
        if (room.getStatus() == com.example.hotelmanagement.enums.RoomStatus.BEING_SERVICED) {
            room.changeStatus(com.example.hotelmanagement.enums.RoomStatus.AVAILABLE);
        }
    }

    public String getDescription() { return description; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public Duration getDuration() { return duration; }
    public Housekeeper getHouseKeeper() { return houseKeeper; }
    public HousekeepingStatus getStatus() { return status; }
}
