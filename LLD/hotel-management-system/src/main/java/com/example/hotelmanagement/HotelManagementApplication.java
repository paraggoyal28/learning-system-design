package com.example.hotelmanagement;

import com.example.hotelmanagement.enums.*;
import com.example.hotelmanagement.model.*;
import com.example.hotelmanagement.notification.ConsoleNotificationService;
import com.example.hotelmanagement.service.HotelManagementService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A runnable walkthrough of the main Hotel Management System use cases. */
public class HotelManagementApplication {
    public static void main(String[] args) {
        Address hotelAddress = new Address("MI Road", "Jaipur", "Rajasthan", "302001", "India");
        Hotel hotel = new Hotel("The Example Palace");
        HotelLocation jaipur = new HotelLocation("Jaipur-Central", hotelAddress);
        jaipur.addRoom(new Room("101", RoomStyle.STANDARD, RoomStatus.AVAILABLE, new BigDecimal("3500.00"), false));
        jaipur.addRoom(new Room("201", RoomStyle.DELUXE, RoomStatus.AVAILABLE, new BigDecimal("5200.00"), false));
        jaipur.addRoom(new Room("301", RoomStyle.FAMILY_SUITE, RoomStatus.AVAILABLE, new BigDecimal("8000.00"), false));
        hotel.addLocation(jaipur);

        Address guestAddress = new Address("C-Scheme", "Jaipur", "Rajasthan", "302001", "India");
        Guest guest = new Guest("Aarav Sharma", guestAddress, "aarav@example.com", "+91-9000000000",
                new Account("G-100", "demo-password", AccountType.GUEST));
        Receptionist receptionist = new Receptionist("Riya", hotelAddress, "riya@hotel.example", "+91-9000000001",
                new Account("E-101", "demo-password", AccountType.RECEPTIONIST));
        Server server = new Server("Kabir", hotelAddress, "kabir@hotel.example", "+91-9000000002",
                new Account("E-102", "demo-password", AccountType.SERVER));
        Housekeeper housekeeper = new Housekeeper("Meera", hotelAddress, "meera@hotel.example", "+91-9000000003",
                new Account("E-103", "demo-password", AccountType.HOUSEKEEPER));
        Manager manager = new Manager("Dev", hotelAddress, "dev@hotel.example", "+91-9000000004",
                new Account("E-104", "demo-password", AccountType.MANAGER));
        manager.addWorker(receptionist);
        manager.addWorker(server);
        manager.addWorker(housekeeper);

        HotelManagementService service = new HotelManagementService(hotel, new ConsoleNotificationService());
        LocalDate firstStay = LocalDate.now().plusDays(2);

        System.out.println("\n=== 1. Search available rooms ===");
        service.searchRooms("Jaipur-Central", RoomStyle.STANDARD, firstStay, 2)
                .forEach(System.out::println);

        System.out.println("\n=== 2. Book a room ===");
        RoomBooking booking = service.bookRoom(guest, "Jaipur-Central", RoomStyle.STANDARD, firstStay, 2);
        System.out.println(booking.fetchDetails());

        System.out.println("\n=== 3. Add amenities and room-service charges ===");
        service.addRoomCharge(booking.getReservationNumber(),
                new Amenity("Airport pickup", "One-way airport transfer", new BigDecimal("900.00")));
        server.addRoomCharge(booking,
                new RoomService(true, LocalDateTime.now(), "Laundry service", new BigDecimal("450.00")));
        server.addRoomCharge(booking, new KitchenService("Dinner for one", new BigDecimal("750.00")));
        System.out.println(booking.getInvoice().print());

        System.out.println("\n=== 4. Pay invoice and check in ===");
        Payment payment = service.payInvoice(booking.getReservationNumber(), PaymentMethod.CREDIT_CARD);
        System.out.println("Payment " + payment.getPaymentId() + " status: " + payment.getStatus());
        service.sendCheckOutReminder(booking.getReservationNumber());
        RoomKey key = service.checkIn(booking.getReservationNumber());
        System.out.println("Issued room key: " + key.getKeyId() + " (active=" + key.isActive() + ")");

        System.out.println("\n=== 5. Check out and housekeeping ===");
        Invoice finalInvoice = service.checkOut(booking.getReservationNumber());
        System.out.println(finalInvoice.print());
        RoomHouseKeeping task = service.createHousekeepingTask("101", "Jaipur-Central",
                "Change linen and clean bathroom", Duration.ofMinutes(45), housekeeper);
        System.out.println("Housekeeping status: " + task.getStatus());
        service.completeHousekeepingTask("101", "Jaipur-Central", task);
        System.out.println("Housekeeping completed; room status: " + jaipur.findRoom("101").orElseThrow().getStatus());

        System.out.println("\n=== 6. Cancel another booking and apply the refund rule ===");
        RoomBooking cancellable = service.bookRoom(guest, "Jaipur-Central", RoomStyle.DELUXE,
                LocalDate.now().plusDays(10), 1);
        Payment cancellationPayment = service.payInvoice(cancellable.getReservationNumber(), PaymentMethod.CASH);
        BigDecimal refund = service.cancelBooking(cancellable.getReservationNumber(), LocalDateTime.now());
        System.out.println("Cancelled " + cancellable.getReservationNumber()
                + "; payment=" + cancellationPayment.getStatus() + "; refunded=" + refund);

        System.out.println("\n=== Demo summary ===");
        System.out.println("Hotel: " + hotel.getName() + " | location: " + jaipur.getName());
        System.out.println("Guest bookings: " + guest.getBookings().size());
        System.out.println("Guest completed check-ins: " + guest.getTotalRoomsCheckedIn());
        System.out.println("Manager workers: " + manager.getWorkers().size());
        System.out.println("Room key active after checkout: " + key.isActive());
    }
}
