package model.reservation;

import java.util.UUID;

public class Service {
    private String id;
    private String name;
    private double price;

    public Service(String name, double price) {
        this.id = "SER-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
