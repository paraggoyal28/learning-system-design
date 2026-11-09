package com.example.hotelmanagement.model;

import com.example.hotelmanagement.enums.AccountStatus;
import com.example.hotelmanagement.enums.AccountType;
import java.util.Objects;

public class Account {
    private final String id;
    // Demo only: production systems must store a salted password hash, never plain text.
    private String password;
    private AccountStatus status;
    private final AccountType accountType;

    public Account(String id, String password, AccountType accountType) {
        this.id = Objects.requireNonNull(id);
        this.password = Objects.requireNonNull(password);
        this.accountType = Objects.requireNonNull(accountType);
        this.status = AccountStatus.ACTIVE;
    }

    public String getId() { return id; }
    public AccountStatus getStatus() { return status; }
    public AccountType getAccountType() { return accountType; }

    public void resetPassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        this.password = newPassword;
    }

    public void changeStatus(AccountStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    public boolean isActive() { return status == AccountStatus.ACTIVE; }
}
