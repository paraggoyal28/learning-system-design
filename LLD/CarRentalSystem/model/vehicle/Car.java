package model.vehicle;

import model.enums.CarType;
import model.enums.VehicleStatus;

public class Car extends Vehicle {

    private CarType carType;

    public Car(String licensePlate, String barcode, int passengerCapacity, double dailyRate, int modelYear, CarType carType) {
        super(licensePlate, barcode, passengerCapacity, dailyRate, modelYear);
        this.carType = carType;
    }

    public CarType getCarType() {
        return carType;
    }

    public void setCarType(CarType carType) {
        this.carType = carType;
    }

    @Override
    public boolean reserveVehicle() {
        if (this.getStatus() == VehicleStatus.AVAILABLE) {
            this.setStatus(VehicleStatus.RESERVED);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Car{" +
                "carType=" + carType +
                '}';
    }
}
