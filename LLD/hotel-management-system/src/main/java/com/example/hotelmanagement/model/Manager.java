package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.AccountType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Manager extends Person {
    private final List<Person> workers = new ArrayList<>();

    public Manager(String name, Address address, String email, String phone, Account account) {
        super(name, address, email, phone, account);
        if (account.getAccountType() != AccountType.MANAGER) {
            throw new IllegalArgumentException("Manager must have MANAGER account type");
        }
    }

    public void addWorker(Person worker) { workers.add(worker); }
    public List<Person> getWorkers() { return Collections.unmodifiableList(workers); }
}
