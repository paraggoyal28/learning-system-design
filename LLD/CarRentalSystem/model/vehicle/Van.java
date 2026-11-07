package model.vehicle;

import model.enums.VanType;
import model.enums.VehicleStatus;

public class Van extends Vehicle {
    private VanType vanType;

    public Van(String licensePlate, String barcode, int passengerCapacity, double dailyRate, int modelYear, VanType vanType) {
        super(licensePlate, barcode, passengerCapacity, dailyRate, modelYear);
        this.vanType = vanType;
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
        return "Van{" +
                "vanType=" + vanType +
                '}';
    }
}
