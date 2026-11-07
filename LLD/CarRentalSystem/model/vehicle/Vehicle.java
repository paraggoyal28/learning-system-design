package model.vehicle;

import model.enums.VehicleStatus;

public abstract class Vehicle {
    private String licensePlate;
    private String barcode;
    private int passengerCapacity;
    private VehicleStatus status;
    private double dailyRate;
    private int modelYear;

    public Vehicle(String licensePlate, String barcode, int passengerCapacity, double dailyRate, int modelYear) {
        this.licensePlate = licensePlate;
        this.barcode = barcode;
        this.passengerCapacity = passengerCapacity;
        this.status = VehicleStatus.AVAILABLE;
        this.dailyRate = dailyRate;
        this.modelYear = modelYear;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(int passengerCapacity) {
        this.passengerCapacity = passengerCapacity;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(double dailyRate) {
        this.dailyRate = dailyRate;
    }

    public int getModelYear() {
        return modelYear;
    }

    public void setModelYear(int modelYear) {
        this.modelYear = modelYear;
    }

    public abstract boolean reserveVehicle();

    @Override
    public String toString() {
        return "Vehicle{" +
                "licensePlate='" + licensePlate + '\'' +
                ", barcode='" + barcode + '\'' +
                ", passengerCapacity=" + passengerCapacity +
                ", status=" + status +
                ", dailyRate=" + dailyRate +
                ", modelYear=" + modelYear +
                '}';
    }
}
