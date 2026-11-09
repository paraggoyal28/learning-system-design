package com.example.hotelmanagement.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Hotel {
    private final String name;
    private final List<HotelLocation> locations = new ArrayList<>();

    public Hotel(String name) { this.name = Objects.requireNonNull(name); }
    public String getName() { return name; }
    public void addLocation(HotelLocation location) { locations.add(Objects.requireNonNull(location)); }
    public List<HotelLocation> getLocations() { return Collections.unmodifiableList(locations); }
    public Optional<HotelLocation> getLocation(String name) {
        return locations.stream().filter(l -> l.getName().equalsIgnoreCase(name)).findFirst();
    }
}
