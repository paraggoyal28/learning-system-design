package com.example.hotelmanagement.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class RoomKey {
    private final String keyId;
    private final String barcode;
    private final LocalDateTime issuedAt;
    private boolean active;
    private final boolean master;
    private Room assignedRoom;

    public RoomKey(String keyId, String barcode, boolean active, boolean master) {
        this.keyId = Objects.requireNonNull(keyId);
        this.barcode = Objects.requireNonNull(barcode);
        this.issuedAt = LocalDateTime.now();
        this.active = active;
        this.master = master;
    }

    public void assignRoom(Room room) {
        this.assignedRoom = Objects.requireNonNull(room);
        room.addKey(this);
    }

    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public String getKeyId() { return keyId; }
    public String getBarcode() { return barcode; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public boolean isMaster() { return master; }
    public Room getAssignedRoom() { return assignedRoom; }
}
