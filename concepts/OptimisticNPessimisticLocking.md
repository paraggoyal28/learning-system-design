# Design Document: Concurrency Control Strategies (Optimistic Vs. Pessimistic Locking)

## Problem Statement
In high-concurrency environments (e.g inventory management, ticket booking, financial ledgers), 
multiple clients frequently attempt to modify the same resource simultaneously. Without proper 
concurrency control, system face:
* **Lost Updates**:  Transaction A overwrites Transaction B's committed work
* **Dirty Reads**: Reading uncommitted data that may be rolled back
* **Non-Repeatable Reads**: Data changing between two reads in the same transaction

## Strategy 1: Pessimistic Locking

### Concept
Assumes conflict is *likely*
Prevents conflict by acquiring a lock on the data before processing it.
Other transactions attempting to access the same data are blocked until the lock is released

### Implementation Patterns

#### A. Database Level (SELECT FOR UPDATE)
Delegating to database engine

BEGIN TRANSACTION;
-- Acquires an exclusive lock (X-Lock) on the row
SELECT stock_count FROM inventory WHERE product_id = 123 FOR UPDATE; 

-- Application logic (check stock, calculate new value)
UPDATE inventory SET stock_count = stock_count - 1 WHERE product_id = 123;

COMMIT; -- Lock is released here

#### B. Application Level (Distributed Lock)
For distributed systems where data spans multiple stores (e.g updating Redis + MySQL), a 
distributed lock manager (DLM) like Redis (Redlock) or Zookeeper is used

Lock lock = redisDistLock.acquire("lock:product:123", timeout=5s);
if (lock == null) throw new OptimizationException("Resource busy");
try {
    // Critical section
    performComplexUpdate();
} finally {
    lock.release();
}

#### Pros
* **Prevents conflicts**: Guarantees serialized access, useful for high-contention data
* **Simpler Application Logic**: No need for retry loops or complex merge logic.
* **Data Integrity**: Strict consistency is easier to enforce

#### Cons
* **Deadlocks**: High risk if multiple resources are locked in different orders
* **Performance Bottleneck**: Reduces concurrency, waiting transactions consume connections
* **Resource Holding**: Locks are held for the duration of the network calls/processing

#### Use Cases
* **Long running transactions**: When the cost of retrying is too high
* **High Contention**: "Flash sale" scenarios where 1000 users fight for 1 item
* **Strict ordering requirements**: First-come-first-served queues

## Strategy 2: Optimistic Locking

### Concept
Assumes conflict is *unlikely*
Does not lock the record during the read or processing phase.
Validates the data hasn't changed before committing the update

### Implementation Patterns

#### A. Version Column (Standard)
Add a version (integer) or last_updated_timestamp column to the table

-- Step 1: Read data
SELECT id, stock_count, version FROM inventory WHERE id = 123;
-- Result: stock=10, version=5

-- Step 2: Application logic
new_stock = 9;

-- Step 3: Atomic Compare-and-Swap (CAS)
UPDATE inventory 
SET stock_count = 9, version = version + 1 
WHERE id = 123 AND version = 5;

#### B. Conditional Update (No Version Column)
Used when the update condition depends on the value itself (e.g ensuring stock doesn't go 
negative).

UPDATE inventory 
SET stock_count = stock_count - 1 
WHERE id = 123 AND stock_count > 0;

*Note: susceptible to ABA problem if not careful, though usually acceptable for simple counters*

#### C. Handling Failures (Retry loop)
Simple optimistic locking allows failures, the application **must** handle
*OptimisticLockException*

int maxRetries = 3;
while (maxRetries > 0) {
    try {
        Data d = repo.findById(123);
        d.updateField(newValue);
        repo.save(d); // Contains "WHERE version = ?" check
        break; // Success
    } catch (OptimisticLockException e) {
        maxRetries--;
        if (maxRetries == 0) throw e;
        backoff(); // Optional jitter
    }
}

#### Pros
* **High Concurrency**
* **Deadlock free**
* **Scalable**: Better for read-heavy workflows

#### Cons
* **Wasted Work**: If conflicts are frequent, retries will waste CPU and DB cycles
* **Complexity**: Application must implement robust retry logic/jitter
* **User Experience**: Might require informing the user "Data changed, please reload"

#### Use Cases
* **Read-Heavy/Write-Rare**: User profile updates, editing wiki pages
* **Low Contention**: Generate e-commerce "add to cart" (if stock is ample)
* **Distributed Systems**: When holding a DB transaction open across microservices is an
antipattern

## Comparison


|Feature|Pessimistic Locking|Optimistic Locking|
|----|-----|-----|
|Throughput|Low (Blocking)|High (Non-blocking)|
|Cost of Conflict|Low (Wait time)|High (Retry entire operation)|
|Transaction Length|Must be short|Can be long (processing happens outside DB txn)|
|Best For|High Contention (N writers)|"Low Contention (1 writer, N readers)"|
|Deadlock Risk|High|Low|







