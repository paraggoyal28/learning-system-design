package model.vehicle;

import model.address.Address;
import model.enums.VehicleStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Location {
    private String locationId;
    private String name;
    private Address address;
    private List<Vehicle> vehicles = new ArrayList<>();

    public Location(String name, Address address) {
        this.locationId = "LOC-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();;
        this.name = name;
        this.address = address;
    }

    public void addVehicle(Vehicle vehicle) {
        this.vehicles.add(vehicle);
    }



    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }
}


