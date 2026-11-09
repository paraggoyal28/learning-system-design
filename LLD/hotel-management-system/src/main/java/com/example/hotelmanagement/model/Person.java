package com.example.hotelmanagement.model;

import java.util.Objects;

public abstract class Person {
    private final String name;
    private final Address address;
    private final String email;
    private final String phone;
    private final Account account;

    protected Person(String name, Address address, String email, String phone, Account account) {
        this.name = Objects.requireNonNull(name);
        this.address = Objects.requireNonNull(address);
        this.email = Objects.requireNonNull(email);
        this.phone = Objects.requireNonNull(phone);
        this.account = Objects.requireNonNull(account);
    }

    public String getName() { return name; }
    public Address getAddress() { return address; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Account getAccount() { return account; }
}
