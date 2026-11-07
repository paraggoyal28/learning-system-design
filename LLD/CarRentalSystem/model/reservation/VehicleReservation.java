package model.reservation;

import model.enums.ReservationStatus;
import model.person.Member;
import model.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VehicleReservation {
    private String reservationNumber;
    private LocalDate creationLocalDate;
    private LocalDate dueLocalDate;
    private LocalDate returnLocalDate;
    private ReservationStatus status;
    private Vehicle vehicle;
    private Member member;
    private List<Equipment> equipmentList = new ArrayList<>();
    private List<Service>  serviceList = new ArrayList<>();
    private double totalAmount;
    private static HashMap<String, VehicleReservation> vehicleReservationHashMap = new HashMap<>();

    public VehicleReservation(String reservationNumber,
                              LocalDate dueLocalDate,
                              Vehicle vehicle,
                              Member member) {
        this.reservationNumber = reservationNumber;
        this.creationLocalDate = LocalDate.now();
        this.dueLocalDate = dueLocalDate;
        this.status = ReservationStatus.PENDING;
        this.vehicle = vehicle;
        this.member = member;
        this.vehicleReservationHashMap.put(reservationNumber, this);
        calculateTotalAmount();
    }

    public void addEquipment(Equipment equipment) {
        equipmentList.add(equipment);
        calculateTotalAmount();
    }

    public void addService(Service service) {
        serviceList.add(service);
        calculateTotalAmount();
    }

    private double calculateTotalAmount() {
        long diffInMillis = Math.abs(dueLocalDate.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC) - creationLocalDate.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC));
        long days = Math.max(1, diffInMillis/(24 * 60 * 60));
        double baseRate = vehicle.getDailyRate() * days;
        double equipmentTotal = equipmentList.stream().mapToDouble(Equipment::getPrice).sum();
        double serviceTotal = serviceList.stream().mapToDouble(Service::getPrice).sum();

        this.totalAmount = baseRate + equipmentTotal + serviceTotal;
        return this.totalAmount;
    }

    public static VehicleReservation fetchReservationDetails(String reservationNumber) {
        // Mock database retrieval logic
        System.out.println("Fetching reservation details for " +  reservationNumber);
        return vehicleReservationHashMap.get(reservationNumber);
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getReservationNumber() {
        return reservationNumber;
    }

    public void setReservationNumber(String reservationNumber) {
        this.reservationNumber = reservationNumber;
    }

    public LocalDate getCreationLocalDate() {
        return creationLocalDate;
    }

    public void setCreationLocalDate(LocalDate creationLocalDate) {
        this.creationLocalDate = creationLocalDate;
    }

    public LocalDate getDueLocalDate() {
        return dueLocalDate;
    }

    public void setDueLocalDate(LocalDate dueLocalDate) {
        this.dueLocalDate = dueLocalDate;
    }

    public LocalDate getReturnLocalDate() {
        return returnLocalDate;
    }

    public void setReturnLocalDate(LocalDate returnLocalDate) {
        this.returnLocalDate = returnLocalDate;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public List<Equipment> getEquipmentList() {
        return equipmentList;
    }

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    public List<Service> getServiceList() {
        return serviceList;
    }

    public void setServiceList(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "VehicleReservation{" +
                "reservationNumber='" + reservationNumber + '\'' +
                ", creationLocalDate=" + creationLocalDate +
                ", dueLocalDate=" + dueLocalDate +
                ", returnLocalDate=" + returnLocalDate +
                ", status=" + status +
                ", vehicle=" + vehicle +
                ", member=" + member +
                ", equipmentList=" + equipmentList +
                ", serviceList=" + serviceList +
                ", totalAmount=" + totalAmount +
                '}';
    }
}

