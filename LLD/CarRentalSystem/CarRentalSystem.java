import model.enums.ReservationStatus;
import model.enums.VehicleStatus;
import model.person.Member;
import model.reservation.Equipment;
import model.reservation.Payment;
import model.reservation.VehicleReservation;
import model.vehicle.Location;
import model.vehicle.Vehicle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CarRentalSystem {
    private String name;
    private List<Location> locations = new ArrayList<>();

    public CarRentalSystem(String name) {
        this.name = name;
    }

    public void addLocation(Location location) {
        locations.add(location);
    }

    public VehicleReservation makeReservation(Member member,
                                              Vehicle vehicle,
                                              LocalDate dueDate,
                                              List<Equipment> equipmentList) {
        if (!vehicle.reserveVehicle()) {
            throw new IllegalStateException("Selected vehicle is not available for reservation");
        }

        String reservationNumber = "RES-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        VehicleReservation reservation = new VehicleReservation(reservationNumber, dueDate, vehicle, member);

        if (equipmentList != null) {
            for (Equipment equipment : equipmentList) {
                reservation.addEquipment(equipment);
            }
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.getVehicle().reserveVehicle();
        System.out.println("Reservation created successfully: " + reservationNumber);
        return reservation;
    }

    public List<Vehicle> getAvailableVehicles() {
        List<Vehicle> available = new ArrayList<>();
        for (Location location : locations) {
            for (Vehicle vehicle : location.getVehicles()) {
                if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
                    available.add(vehicle);
                }
            }
        }
        return available;
    }

    public boolean completeCheckout(VehicleReservation reservation, double paymentAmount) {
        Payment payment = new Payment("PAY-" + UUID.randomUUID().toString().substring(0, 6), paymentAmount);
        if (payment.makePayment()) {
            reservation.setStatus(ReservationStatus.COMPLETED);
            reservation.getVehicle().setStatus(VehicleStatus.LOANED);
            System.out.println("Checkout completed for vehicle: " + reservation.getVehicle().getLicensePlate());
            return true;
        }
        reservation.setStatus(ReservationStatus.CANCELED);
        return false;
    }
}
