# Smart Elevator System

## System Overview
The system manages N elevators serving M floors. Handles requests from inside the
elevators (Internal Requests) and from floor hallways (External Requests). The goal
is to maximize throughput and minimize the wait time using efficient scheduling

## Class Architecture & Relationships
System consists of Hardware components (managed by software) and Control Logic
**Core Classes**
1. **ElevatorSystem(Singleton)**: The main entry point. It holds the list of all elevators and the Dispatcher
2. **ElevatorController**: The "brain" of the elevator. Decides the next move based on the assigned requests
3. **ElevatorCar**: The physical entity. It holds state (currentFloor, direction, status)
4. **Dispatcher**: Routing logic. Decided which elevator gets an external hall request.
5. **Button**: Interface for internal (inside car) and External (hallway) inputs
6. **Display**: Observer that updates floor numbers

## Design Pattern Implementation

### Strategy Pattern (For Dispatching Logic)
**Why**: You might want different algorithms for different building types (eg. a hospital 
needs "Nearest Available", while a skyscraper uses "Zoning")

**Implementation**:
// 1. The Strategy Interface
class DispatchStrategy {
public:
    virtual int selectElevator(int sourceFloor, Direction dir,
                                const vector<ElevatorController*>& controllers) = 0;
}

// 2. Concrete Strategy: Odd-Even(simple)
class OddEvenStrategy: public DispatchStrategy {
public:
    int selectElevator(int sourceFloor, Direction dir,
                        const vector<ElevatorController*>& controllers) override 
    {
        // Odd floors go to Odd elevators, Even to Even
        // Fallback to nearest if dedicated one is out of order
        // Implementation logic...      
    }
}

// 3. Concrete Strategy: Least Wait Time (Complex/Standard)
class LeastWaitTimeStrategy: public DispatchStrategy {
public:
    int selectElevator(int sourceFloor, Direction dir,
                        const vector<ElevatorController*>& controllers) override 
    {
        int bestElevatorId = -1;
        int minCost = INT_MAX;

        for (auto& ctrol: controllers) {
            int cost = ctrl->calculateCost(sourceFloor, dir);
            if (cost < minCost) {
                minCost = cost;
                bestElevatorId = ctrl->getID();
            }
        }
        return bestElevatorId;
    }
};

### Observer Pattern (For Displays)
**Why**: When an elevator moves, displays inside the car and on every floor need to update.
The elevator shouldn't "know" about every display hardware; it should just broadcast changes

**Implementation**: The *ElevatorCar* is the Subject, and the *Display* is the Observer

// 1. Observer Interface
class DisplayObserver {
    public:
        virtual void update(int floor, Direction dir) = 0;
}

// 2. Concrete Observer : Hallway Display
class HallDisplay: public DisplayObserver {
    int floorId;
    public:
        void update(int floor, Direction dir) override {
            // Hardware command to update LED system
            print("Floor " + to_string(floorId) + " Display elevator is at " + to_string(floor)); 
        }
}

// 3. Subject: Elevator Car
class ElevatorCar {
    vector<DisplayObserver*> displays; // list of subscribers
    int currentFloor;
    Direction currentDirection;

    public:

        void addObserver(DisplayObserver* obs) {
            displays.push_back(obs);
        }

        void move(int floor) {
            currentFloor = floor;
            notifyObservers();
        }

        void notifyObservers() {
            for (auto d: displays) {
                d->update(currentFloor, currentDir);
            }
        }
};

## Deep Dive: ElevatorController & SCAN Algorithm

This is the core logic. To handle the "SCAN" (elevator) algorithm efficiently, we can use 
two std:set data structures (ordered set).

* upStops: Keep requested floors sorted in ascending order (eg. 2, 5, 8)
* downStops: Keeps requested floors sorted in descending order (eg. 9, 4, 1)

The Logic Flow: 
1. **Aceept Request**: Add the floor to the appropriate set based on the current location and
direction
2. **Process**: If moving UP, keep popping from *upStops* until empty, and then switch direction

**Code Implementation**

#include <set>
#include <iostream>
using namespace std;

enum DIRECTION { UP, DOWN, IDLE };

class ElevatorController {
    int currentFloor;
    Direction currentDirection;

    // Sets automatically keep floors sorted and unique
    set<int> upStops;
    set<int, greater<int>> downStops; // Max heap behaviour for downward processing

public:
    ElevatorController(int startFloor): currentFloor(startFloor), currentDirection(DIRECTION.IDLE) {}

    // 1. Logic to accept request
    void acceptRequest(int destinationFloor) {
        if (destinationFloor == currentFloor) return;

        if (destinationFloor > currentFloor) {
            downStops.insert(destinationFloor);
            if (currentDirection == DIRECTION.IDLE)
                currentDirection = DIRECTION.DOWN;
        } else {
            upStops.insert(destinationFloor);
            if (currentDirection == DIRECTION.IDLE)
                currentDirection = DIRECTION.UP;
        }
    }

    // 2. LOGIC to process movement (SCAN algorithm) 
    void processNextMove() {

        if (currentDirection == DIRECTION.IDLE) return;

        else if (currentDirection == DIRECTION.DOWN) {

            if (downStops.empty()) {
                if (!upStops.empty()) {
                    currentDirection = DIRECTION.UP;
                    processNextMove();
                } else {
                    // No requests
                    currentDirection = IDLE;
                }
                return;
            }

            int nextFloor = *downStops.begin();
            downStops.erase(downStops.begin());
            moveTo(nextFloor);
        }

        else {

            if (upStops.empty()) {
                if (!downStops.empty()) {
                    currentDirection = DIRECTION.DOWN;
                    processNextMove();
                } else {
                    currentDirection = IDLE;
                }
                return;
            }

            int nextFloor = *upStops.begin();
            upStops.erase(upStops.begin());
            moveTo(nextFloor);
        }
    }

    void moveTo(int floor) {
        cout << "Moving To: " << currentFloor << " to " << floor << "...." << endl;
        currentFloor = floor;
        // Notify Observers (Displays) here
        // Open Doors logic here
    }
};

## Handling Multiple Elevators (The Full Flow)

When a user presses a button in the hallway (External Request), the *Elevator System*
coordinates the action.

1. **User Action**: User at Floor 5 presses "UP".
2. **System Entry**: ElevatorSystem.handleExternalRequest(5, UP) is called
3. **Dispatcher Selection**: 
    * The *Dispatcher* iterates through all N *ElevatorControllers*
    * It runs the *active* DispatchStrategy (eg. LeastWaitTimeStrategy)
    * **Cost Calculation**:
        * If ElevatorA is at Floor1 going UP, cost is (5-1) = 4
        * If ElevatorB is at Floor7 going UP, cost is high as it has to serve pending requests
            and then come down
        * Dispatcher selects ElevatorA
4. **Assignment**: *ElevatorController[A].acceptRequest(5)* is called
5. **Execution**: ElevatorA adds floor5 to its *upStops* set and eventually stops there

## Concurrency & Thread Safety
Imagine this scenario:

Thread A (Hardware Input): A user on Floor 5 presses "UP".

Thread B (Hardware Input): A user on Floor 3 presses "UP" at the exact same millisecond.

Thread C (Elevator Engine): The elevator is currently moving and checking the list of stops to decide where to go next.

All three threads are trying to read or write to the shared data structures (upStops and downStops). Without protection, this leads to Race Conditions (data corruption) or Undefined Behavior (crashes).

### The Problem: Race Conditions
If Thread A and Thread B try to insert into upStops simultaneously, the internal structure of the std::set (usually a Red-Black Tree) might get corrupted. Thread C might read the set while it is being rebalanced, causing the elevator to go to a non-existent floor.

### The Solution: Mutex (Mutual Exclusion)
To fix this, we use a Mutex. Think of a Mutex as a "key" to a room. Only one thread can hold the key at a time.

When a thread wants to access the variables, it must lock() (take the key).

If the key is gone, it must wait.

When finished, it must unlock() (return the key).

#include <mutex>
#include <thread>
#include <set>
#include <condition_variable>

class ThreadSafeElevatorController {
    std::set<int> upStops;
    std::set<int, greater<int>> downStops;
    
    // The "Key" to protect the data
    std::mutex mtx;

public:
    void acceptRequest(int floor) {
        // 1. ACQUIRE LOCK
        // std::lock_guard automatically locks when created and unlocks when scope ends.
        // This prevents deadlocks if an error occurs.
        std::lock_guard<std::mutex> lock(mtx);

        // 2. CRITICAL SECTION (Safe to modify data)
        if (floor > currentFloor) {
            upStops.insert(floor);
        } else {
            downStops.insert(floor);
        }
        
        // Lock releases automatically here
    }

    void processNextMove() {
        // 1. ACQUIRE LOCK
        std::lock_guard<std::mutex> lock(mtx);

        // 2. CRITICAL SECTION (Safe to read/modify data)
        if (!upStops.empty()) {
            int next = *upStops.begin();
            upStops.erase(upStops.begin());
            // Move logic...
        }
    }
};

### The Optimization: Condition Variable (Solving "Bust Waiting")
The code above is "safe" but inefficient. The Elevator Engine (Thread C) runs in an 
infinite loop. If there are no requests, it will constantly acquire the lock, check if 
its empty, release the lock and repeat. This is called Bust Waiting

To fix this, we use **Condition Variables** This allows the thread to "Sleep" until 
a new request arrives.

* **Producer (Button presses)**: Adds data and sends a **Signal**(notify)
* **Consumer (Elevator Engine)**: **Waits** for the signal and sleeps to save CPU

**Implementation**
class OptimizedElevatorController {
    std::set<int> upStops;
    std::mutex mtx;
    std::condition_variable cv; // "Signal" mechanism
    bool running = true;

public:

    // PRODUCER: The user presses a button
    void acceptRequest(int floor) {
        {
            std::lock_guard<std::mutex> lock(mtx);
            upStops.insert(floor);
            std::cout << "Request added for the floor: " << floor << endl;
        } // lock is released before notifying

        // Wake up the worker thread!
        cv.notify_one();
    }

    // CONSUMER: The Elevator processing loop
    void startEngine() {
        while (running) {
            /* unique lock is required for condition_variable (more flexible in terms of lock_guard)
            */

            std::unique_lock<std::mutex> lock(mtx);

            // WAIT until: upStops is NOT empty
            // This puts the thread to SLEEP. It consumes 0% CPU while waiting
            // It automatically releases the mutex while sleeping so others can add requests
            // When notified, it re-acquires the mutex and checks the condition
            cv.wait(lock, [this]{ return !upStops.empty(); });

            // CRITICAL Section. We are awake and have the lock
            int nextFloor = *upStops.begin();
            upStops.erase(upStops.begin());

            // Unlock before doing the long "moving" operator to allow new requests to come in
            lock.unlock();

            moveTo(nextFloor);
        }
    }

    void moveTo(int floor) {
        std::this_thread::sleep_for(std::chrono::seconds(2)); // Simulate travel time
        std::cout << "Arrived at floor " << floor << endl;
    }
}






