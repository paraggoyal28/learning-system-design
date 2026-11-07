package model.person;

import model.address.Address;
import model.enums.AccountStatus;

import java.util.UUID;

public class Member extends Person {
    private String memberId;
    private AccountStatus status;

    public Member(String name, Address address, String email, String phone, AccountStatus status) {
        super(name, address, email, phone);
        this.memberId = "MEM-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();;
        this.status = status;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Member{" +
                "memberId='" + memberId + '\'' +
                ", status=" + status +
                ", name='" + name + '\'' +
                ", address=" + address +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
