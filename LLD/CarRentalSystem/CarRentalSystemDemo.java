import model.address.Address;
import model.enums.AccountStatus;
import model.enums.CarType;
import model.enums.VanType;
import model.person.Member;
import model.reservation.VehicleReservation;
import model.vehicle.Car;
import model.vehicle.Location;
import model.vehicle.Van;
import model.vehicle.Vehicle;


import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CarRentalSystemDemo {
    public static void main(String[] args) {

        CarRentalSystem myCarRentalSystem = new CarRentalSystem("Parag Garage");

        Location carRentalSystemLocation =
                new Location("Jaipur", new Address("malaviya nagar",
                        "jaipur",
                        "Rajasthan",
                        "304003",
                        "India"));

        myCarRentalSystem.addLocation(carRentalSystemLocation);

        Vehicle car1 = new Car("LIC" + UUID.randomUUID().toString().substring(0, 6), UUID.randomUUID().toString().substring(0, 4), 2, 40, 1989, CarType.COMPACT);
        Vehicle car2 = new Car("LIC" + UUID.randomUUID().toString().substring(0, 6), UUID.randomUUID().toString().substring(0, 4), 4, 50, 1990, CarType.ECONOMY);
        Vehicle car3 = new Car("LIC" + UUID.randomUUID().toString().substring(0, 6), UUID.randomUUID().toString().substring(0, 4), 6, 120, 1997, CarType.LUXURY);
        Vehicle car4 = new Car("LIC" + UUID.randomUUID().toString().substring(0, 6), UUID.randomUUID().toString().substring(0, 4), 4, 30, 1934, CarType.MIDSIZE);
        Vehicle car5 = new Car("LIC" + UUID.randomUUID().toString().substring(0, 6), UUID.randomUUID().toString().substring(0, 4), 6, 100, 1943, CarType.SUV);
        Vehicle van1 = new Van("LIC" + UUID.randomUUID().toString().substring(0, 6), UUID.randomUUID().toString().substring(0, 4), 5, 20, 1892, VanType.CARGO);
        Vehicle van2 = new Van("LIC" + UUID.randomUUID().toString().substring(0, 6), UUID.randomUUID().toString().substring(0, 4), 5, 30, 1897, VanType.PASSENGER);

        carRentalSystemLocation.addVehicle(car1);
        carRentalSystemLocation.addVehicle(car2);
        carRentalSystemLocation.addVehicle(car3);
        carRentalSystemLocation.addVehicle(car4);
        carRentalSystemLocation.addVehicle(car5);
        carRentalSystemLocation.addVehicle(van1);
        carRentalSystemLocation.addVehicle(van2);

        Member member1 = new Member("Chirag",
                new Address("jawahar nagar", "jaipur", "rajasthan", "340056", "india"),
                "chirag@gmail.com",
                "9897989798", AccountStatus.ACTIVE);

        Member member2 = new Member("Vinod",
                new Address("sethi colony", "jaipur", "rajasthan", "302004", "india"),
                "vinod16@gmail.com",
                "9351545234", AccountStatus.ACTIVE);

        List<Vehicle> availableVehicles = myCarRentalSystem.getAvailableVehicles();

        // member1 wants a cheapest vehicle available with at least 4 capacity or if no vehicle with greater than 4 capacity than max capacity vehicle or else null
        Vehicle economyVehicle = availableVehicles.stream().filter(vehicle -> vehicle.getPassengerCapacity() >= 4)
                .min(Comparator.comparingDouble(Vehicle::getDailyRate))
                .orElse(availableVehicles.stream().max(Comparator.comparingDouble(Vehicle::getPassengerCapacity)).orElse(null));

        if (economyVehicle != null) {

            VehicleReservation reservation1 = myCarRentalSystem.makeReservation(member1,
                    economyVehicle, LocalDate.now().plusDays(21), new ArrayList<>());


            System.out.println(
                    "Reservation for member1: " +
                            VehicleReservation.fetchReservationDetails(reservation1.getReservationNumber()));

            double amountToBePaid = reservation1.getTotalAmount();

            myCarRentalSystem.completeCheckout(reservation1, amountToBePaid);
        } else {
            System.out.println("No available vehicles available");
        }

        System.out.println("============================================================");
        System.out.println("=============================================================");

        availableVehicles = myCarRentalSystem.getAvailableVehicles();

        // member1 wants a vehicle with capacity 5 or any vehicle available
        Vehicle suvVehicle = availableVehicles.stream().filter(vehicle -> vehicle.getPassengerCapacity() == 5).findFirst()
                .orElse(availableVehicles.stream().findFirst().orElse(null));

        if (suvVehicle != null) {


            VehicleReservation reservation2 = myCarRentalSystem.makeReservation(member2,
                    suvVehicle, LocalDate.now().plusDays(21), new ArrayList<>());


            System.out.println(
                    "Reservation for member2: " +
                            VehicleReservation.fetchReservationDetails(reservation2.getReservationNumber()));

            double amountToBePaid2 = reservation2.getTotalAmount();

            myCarRentalSystem.completeCheckout(reservation2, amountToBePaid2);
        } else {
            System.out.println("Sorry! No vehicle available");
        }

        System.out.println(myCarRentalSystem.getAvailableVehicles());
    }
}
