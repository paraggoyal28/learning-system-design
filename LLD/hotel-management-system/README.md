# Hotel Management System — Java LLD Demo

A runnable Java 17 implementation based on the class model in the linked *Grokking the Object-Oriented Design Interview* Hotel Management System case study.

The source article provides a **high-level Python skeleton**: many methods are placeholders (`None`), and a few classes named in the design discussion (for example, `Invoice` and `Notification`) are not implemented in its code section. This project keeps the original domain class names and fills in enough behavior for an end-to-end console demo.

## Requirements

- JDK 17 or newer
- Maven 3.8+ for the Maven build (optional; the included verification script only needs the JDK)
- No third-party runtime libraries

## Run the demo

From this directory:

```bash
mvn clean package
java -cp target/classes com.example.hotelmanagement.HotelManagementApplication
```

Or compile and run the dependency-free behavioral checks and demo with the JDK only:

```bash
./scripts/verify.sh
```

The script compiles the main and test sources with Java 17 compatibility, runs a few booking/refund assertions, and then launches the console demo. For just the demo, compile `src/main/java` with `javac` and run `com.example.hotelmanagement.HotelManagementApplication`.

The demo covers room search, booking, adding amenities and room services, invoicing, payment, check-in, key-card issuance, check-out, housekeeping, cancellation and refund, and notifications. `HotelManagementBehaviorTest` checks overlapping inventory, consecutive stays, refund eligibility, and releasing a cancelled reservation.

## Package structure

```text
com.example.hotelmanagement
├── HotelManagementApplication.java       # runnable demo
├── enums/                                # domain states and types
├── exception/                            # application exception
├── model/                                # entities and value objects
├── notification/                         # notification abstraction + console adapter
└── service/                              # use-case orchestration
```

## Main classes and responsibilities

| Class | Responsibility |
|---|---|
| `Hotel`, `HotelLocation` | A hotel can operate multiple locations; each location owns its rooms and bookings. |
| `Room`, `RoomStyle`, `RoomStatus` | Room identity, category, price, operational state, keys, and housekeeping history. |
| `Search` | Search abstraction implemented by `HotelLocation`, where inventory is available. |
| `Account`, `Person`, `Guest`, `Receptionist`, `Server`, `Manager`, `Housekeeper` | Users and staff roles. |
| `RoomBooking`, `BookingStatus` | Reservation dates, guest/room association, lifecycle, key, notifications, and invoice. |
| `RoomKey` | Key-card identity, assignment, and activation state. |
| `RoomHouseKeeping` | Housekeeping work record and state. |
| `RoomCharge`, `Amenity`, `RoomService`, `KitchenService` | Extensible charge hierarchy. Each charge converts itself into an `InvoiceItem`. |
| `Invoice`, `InvoiceItem`, `Payment` | Invoice line items, totals, payment method/status, and refund handling. |
| `Notification`, `NotificationService` | Notification data and pluggable delivery; demo uses console output. |
| `HotelManagementService` | Coordinates search, booking, payment, check-in/out, cancellation, housekeeping, and notifications. |

## Key design decisions

1. **Date-based availability:** A room is available when it is operationally `AVAILABLE` and no confirmed or checked-in booking overlaps the requested half-open date interval `[checkIn, checkOut)`. This allows consecutive stays where one guest checks out on the date another checks in.
2. **Booking lifecycle:** The demo uses `CONFIRMED → CHECKED_IN → CHECKED_OUT`, or `CONFIRMED → CANCELLED`.
3. **Refund rule:** A full refund is given when cancellation occurs strictly before 24 hours prior to the scheduled 3:00 PM check-in time. The rule is centralized in `RoomBooking.isEligibleForFullRefund`.
4. **Charges:** Room price is added when the booking is created. Extra services add invoice items before payment. Adding charges after a fully paid invoice is rejected.
5. **Payments:** Credit card, check, and cash are modeled, but payment processing is simulated and always succeeds in this demo.
6. **Notifications:** Booking, cancellation, check-in, and check-out events are sent through `NotificationService`. Reminder methods are callable by an external scheduler; this project does not run a background scheduler.
7. **Persistence:** Data lives in memory and is lost when the application exits. The service is an application-layer example, not a production reservation platform.

## Original skeleton vs. completed implementation

The original class names are retained where present. The implementation adds supporting types and behavior required to make the main use cases runnable. `Search` is implemented by `HotelLocation` rather than an individual `Room`, because searching is an inventory responsibility. Additional staff classes and billing/notification classes are included because the requirements and class-diagram discussion refer to them.

## Production considerations deliberately out of scope

This is an educational LLD exercise. A production system would need persistent repositories/database transactions, concurrent room-inventory locking, authentication and password hashing, payment-provider integration, idempotency, audit logs, timezone-aware hotel-local scheduling, cancellation-policy configuration, authorization, and automated notification scheduling.

## Class diagram

See [`docs/class-diagram.mmd`](docs/class-diagram.mmd) for a Mermaid class diagram showing the principal entities, inheritance, composition, and service dependencies.
