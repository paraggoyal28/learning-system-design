# Parking Lot

# Functional Requirements

* User can park vehicle
* User will be assigned a ticket when he enters
* According to the vehicle type, location and duration of parking, a fee will be charged when user leaves
* Location of the vehicle should be the nearest available parking spot
* User can remove a vehicle

## High Level Architecture
Entry Gate -> Ticket Service -> Parking Lot Manager -> Spot Allocation Engine 
-> Exit Gate -> Billing Service -> Payment Service

## Components
* ParkingLot Service
* SpotAllocation Service
* Ticket Service
* Billing Service
* DisplayBoard Service

## Core Entities

Vehicle
User
ParkingSpot
ParkingFloor (having multiple parking spots)
ParkingLot (having multiple parking floors)
Ticket
VehicleType - enum (BIKE, CAR, TRUCK)
SpotType - enum (COMPACT, LARGE, HANDICAPPED)

* Vehicle
- String number_plate (primary key)
- VehicleType type

* TIcket
- String ticketId
- long entryTime
- ParkingSpot spot
- Vehicle vehicle

* ParkingSpot
- id
- SpotType type
- boolean isOccupied
- Vehicle currentVehicle
 
* ParkingFloor
int floorNumber
Map<VehicleType, TreeSet<ParkingSpot>> spots

* ParkingLot
List<ParkingFloor> floors;

## API Design

User enters. Assing a ticket and park the vehicle

POST /park

Api 
* Fetch the vehicle object from the vehicle database given the license number
* According to the vehicle type, it finds the nearest location on a given floor

Request Body: 
{
  "vehicle": {
    "licensePlate": "KA-01-AB-1234",
    "type": "CAR"
  },
  "entryGateId": "GATE_1",
  "preferences": {
    "spotType": "EV",             
    "nearElevator": true
  }
}

public ParkResponse park(ParkRequest request) {

    validate(request);

    if (idempotencyKeyExists(request.key)) {
        return cachedResponse(request.key);
    }

    List<SpotType> allowedTypes = mapVehicleToSpot(request.vehicle.type);

    ParkingSpot spot = allocator.findNearestAvailable(allowedTypes);

    if (spot == null) {
        throw new ParkingFullException();
    }

    lock(spot);
    try {
        spot.assign(request.vehicle);
    } finally {
        unlock(spot);
    }

    Ticket ticket = ticketService.create(request.vehicle, spot);

    return buildResponse(ticket);
}

Ticket ticket = new Ticket(
    UUID.randomUUID().toString(),
    currentTime,
    vehicle,
    spot
);


Response Specification: 

* Success (200 OK)
{
  "ticketId": "TICKET_12345",
  "spot": {
    "id": "F1-S12",
    "floor": 1,
    "type": "COMPACT"
  },
  "entryTime": 1712345678,
  "message": "Parking assigned successfully"
}

* Failure Response
Parking Full (409 Conflict)
{
  "error": "PARKING_FULL",
  "message": "No available spots for vehicle type CAR"
}

* Invalid Request (400)
{
  "error": "INVALID_REQUEST",
  "message": "Vehicle type missing"
}

* High Level Processing Steps

1. Validate Request
2. Check idempotency (optional)
3. Map vehicle -> Eligible Spot Types
4. Check availability
5. Allocate Spot (atomic)
6. Mark spot as occupied
7. Generate ticket
8. Persist ticket + Spot Assignment
9. Return response

* Determining Eligible Types

CAR → COMPACT, LARGE
BIKE → BIKE
TRUCK → LARGE
EV → EV + fallback LARGE

* Sequence Diagram

Client
  |
  |  POST /park
  |------------------------------------>
  |                                     API Gateway
  |                                     |
  |                                     v
  |                              ParkingController
  |                                     |
  |                                     v
  |                              ParkingService
  |                                     |
  |        (1) Validate Request         |
  |------------------------------------>|
  |                                     |
  |        (2) Check Idempotency        |
  |------------------------------------> IdempotencyStore (Redis/DB)
  |                                     |
  |<------------------------------------|
  |                                     |
  |        (3) Get Eligible Spot Types  |
  |------------------------------------> SpotAllocationService
  |                                     |
  |        (4) Fetch Available Spots    |
  |------------------------------------> SpotCache (Redis / In-Memory)
  |                                     |
  |<------------------------------------|
  |                                     |
  |        (5) Allocate Spot (LOCK)     |
  |------------------------------------> Lock Service / DB Lock
  |                                     |
  |        (6) Mark Spot Occupied       |
  |------------------------------------> Spot DB
  |                                     |
  |        (7) Create Ticket            |
  |------------------------------------> TicketService
  |                                     |
  |        (8) Persist Ticket           |
  |------------------------------------> Ticket DB
  |                                     |
  |        (9) Publish Event            |
  |------------------------------------> Event Bus (Kafka)
  |                                     |
  |                                     v
  |                            DisplayBoardService (async)
  |                                     |
  |<------------------------------------|
  |
  |  (10) Return Response (ticket + spot)
  |<------------------------------------|

| Step | Component          | Description                        |
| ---- | ------------------ | ---------------------------------- |
| 1    | Controller         | Input validation                   |
| 2    | Idempotency Store  | Prevent duplicate tickets          |
| 3    | Allocation Service | Map vehicle → spot types           |
| 4    | Cache              | Fetch available spots quickly      |
| 5    | Locking Layer      | Prevent race conditions            |
| 6    | DB                 | Update spot occupancy              |
| 7    | Ticket Service     | Generate ticket                    |
| 8    | DB                 | Persist ticket                     |
| 9    | Event Bus          | Async updates (display, analytics) |
| 10   | Response           | Return ticket + assigned spot      |

Update Display Boards (Async)
Publish event:
SPOT_ALLOCATED → Display Service

Performance Optimizations
* Maintain separate queues for each spot type
* Pre-sort spot types by distance
* Cache available spots in Redis

Design Patterns Used
* Factory  - Vehicle creation
* Singleton - ParkingLot
* Strategy - Pricing, allocation
* Observer - Display board updates











