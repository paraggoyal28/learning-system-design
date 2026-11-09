package com.example.hotelmanagement.model;

import java.util.Objects;

public class Address {
    private final String streetAddress;
    private final String city;
    private final String state;
    private final String zipCode;
    private final String country;

    public Address(String streetAddress, String city, String state, String zipCode, String country) {
        this.streetAddress = Objects.requireNonNull(streetAddress);
        this.city = Objects.requireNonNull(city);
        this.state = Objects.requireNonNull(state);
        this.zipCode = Objects.requireNonNull(zipCode);
        this.country = Objects.requireNonNull(country);
    }

    public String getStreetAddress() { return streetAddress; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }
    public String getCountry() { return country; }

    @Override
    public String toString() {
        return streetAddress + ", " + city + ", " + state + " " + zipCode + ", " + country;
    }
}
