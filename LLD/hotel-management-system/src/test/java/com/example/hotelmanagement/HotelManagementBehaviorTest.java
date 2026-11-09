package com.example.hotelmanagement;

import com.example.hotelmanagement.enums.AccountType;
import com.example.hotelmanagement.enums.PaymentMethod;
import com.example.hotelmanagement.enums.RoomStatus;
import com.example.hotelmanagement.enums.RoomStyle;
import com.example.hotelmanagement.model.Account;
import com.example.hotelmanagement.model.Address;
import com.example.hotelmanagement.model.Guest;
import com.example.hotelmanagement.model.Hotel;
import com.example.hotelmanagement.model.HotelLocation;
import com.example.hotelmanagement.model.Room;
import com.example.hotelmanagement.model.RoomBooking;
import com.example.hotelmanagement.notification.NotificationService;
import com.example.hotelmanagement.service.HotelManagementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Lightweight dependency-free checks; run with scripts/verify.sh. */
public final class HotelManagementBehaviorTest {
    private HotelManagementBehaviorTest() { }

    public static void main(String[] args) {
        Address address = new Address("Street", "Jaipur", "Rajasthan", "302001", "India");
        Hotel hotel = new Hotel("Test Hotel");
        HotelLocation location = new HotelLocation("Test Location", address);
        location.addRoom(new Room("T-1", RoomStyle.STANDARD, RoomStatus.AVAILABLE,
                new BigDecimal("100.00"), false));
        hotel.addLocation(location);

        Guest guest = new Guest("Test Guest", address, "test@example.com", "123",
                new Account("G-1", "not-a-real-password", AccountType.GUEST));
        NotificationService noOpNotifications = notification -> { };
        HotelManagementService service = new HotelManagementService(hotel, noOpNotifications);
        LocalDate requestedStart = LocalDate.now().plusDays(5);

        RoomBooking booking = service.bookRoom(guest, "Test Location", RoomStyle.STANDARD,
                requestedStart, 2);
        require(service.searchRooms("Test Location", RoomStyle.STANDARD, requestedStart, 2).isEmpty(),
                "An overlapping booking must make the room unavailable");
        require(service.searchRooms("Test Location", RoomStyle.STANDARD, requestedStart.plusDays(2), 2).size() == 1,
                "A stay starting on the previous stay's checkout date should be available");

        service.payInvoice(booking.getReservationNumber(), PaymentMethod.CASH);
        LocalDateTime moreThan24HoursBeforeCheckIn = requestedStart.minusDays(1).atTime(14, 59);
        BigDecimal refunded = service.cancelBooking(booking.getReservationNumber(), moreThan24HoursBeforeCheckIn);
        require(refunded.compareTo(new BigDecimal("200.00")) == 0,
                "A cancellation before the 24-hour cutoff should refund the full invoice");
        require(booking.getInvoice().getPaymentStatus().name().equals("REFUNDED"),
                "The payment should be marked as refunded");
        require(service.searchRooms("Test Location", RoomStyle.STANDARD, requestedStart, 2).size() == 1,
                "A cancelled booking must release its room inventory");

        System.out.println("HotelManagementBehaviorTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
