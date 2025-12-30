# Availability vs Consistency: A Guide for SDEs

This document is designed for software engineers transitioning from SDE1 to SDE2. It covers the fundamental concepts of Availability and Consistency in distributed systems, delving into their definitions, measurements, trade-offs (CAP theorem), and common implementation patterns.

## 1. Introduction

In a distributed system, you rarely get everything you want. As systems scale beyond a single node, you inevitably face network partitions and failures. Understanding how to balance keeping your system operational (Availability) vs. keeping your data accurate (Consistency) is a core competency for senior engineers.

## 2. Availability

Availability is the probability that a system is operational and able to serve requests at a specific point in time. It is a measure of system uptime.

### 2.1 Measurement: The "Nines"

Availability is typically measured in percentages of uptime over a given period (usually a year). We often refer to these as "nines".

| Availability | "Nines" | Downtime per Year | Downtime per Day | Example Class |
| :--- | :--- | :--- | :--- | :--- |
| **99.0%** | Two 9s | 3 days, 15 hours | ~14.4 mins | Batch processing, internal tools |
| **99.9%** | Three 9s | 8 hours, 46 mins | ~1.44 mins | Standard web apps, e-commerce |
| **99.99%** | Four 9s | 52 mins, 36 secs | ~8.64 secs | Enterprise grade, Payments |
| **99.999%** | Five 9s | 5 mins, 15 secs | ~0.86 secs | Telco, Pacemakers, Critical Infra |

### 2.2 Calculation Basics

The basic formula for availability is dealing with **Mean Time Between Failures (MTBF)** and **Mean Time To Repair (MTTR)**.

$$ Availability = \frac{MTBF}{MTBF + MTTR} $$

To improve availability, you generally want to:
1.  **Increase MTBF**: Make components fail less often (better hardware, bug-free code).
2.  **Decrease MTTR**: Fix failures faster (automated monitoring, auto-scaling, self-healing).

### 2.3 System Availability Calculation

When building complex systems, you combine components. The way you combine them dramatically impacts overall availability.

#### Sequential Components
If Component A depends on Component B, both must be up for the system to work.
*   **Formula**: $A_{total} = A_1 \times A_2$
*   **Impact**: **Reduces** overall availability.
*   **Example**: If Service X (99%) calls Database Y (99%), total availability is $0.99 \times 0.99 = 98.01\%$. The system is less available than its weakest link.

#### Parallel Components (Redundancy)
If Component A and Component B do the same job and the system works if *at least one* is up.
*   **Formula**: $A_{total} = 1 - (1 - A_1) \times (1 - A_2)$
*   (This is $1 - \text{Probability both fail}$)
*   **Impact**: **Increases** overall availability.
*   **Example**: If Server 1 (99%) and Server 2 (99%) are load balanced, total availability is $1 - (0.01 \times 0.01) = 1 - 0.0001 = 99.99\%$.
*   **Takeaway**: This is why we add redundancy (replicas) to increase uptime.

---

## 3. Consistency

Consistency, in the context of distributed systems, refers to the uniformity of data across all replicas. When a user reads data, do they see the absolute latest write?

### 3.1 The "Single System Image" Illusion
Ideally, a distributed system should look like one single computer to the user. If I write `X = 10` on Node A, and you immediately read `X` from Node B, you should see `10`. If you see `10`, the system is consistent. If you see the old value, it is inconsistent.

---

## 4. The Trade-off: CAP Theorem

The CAP Theorem states that in a distributed data store, you can primarily guarantee only two of the following three:

1.  **Consistency (C)**: Every read receives the most recent write or an error.
2.  **Availability (A)**: Every request receives a (non-error) response, without the guarantee that it contains the most recent write.
3.  **Partition Tolerance (P)**: The system continues to operate despite an arbitrary number of messages being dropped/delayed by the network between nodes.

### The Realistic Choice: CP vs AP
In a distributed system, **Partition Tolerance (P) is mandatory** (networks fail). Therefore, you must choose between:
*   **CP (Consistency + Partition Tolerance)**: If a partition occurs, shut down non-consistent nodes or refuse writes to maintain data safety. (System goes down or becomes read-only).
*   **AP (Availability + Partition Tolerance)**: If a partition occurs, keep accepting writes even if nodes are out of sync. (System stays up, but data might be stale).

---

## 5. Consistency Patterns

Different use cases require different levels of consistency.

### 5.1 Strong Consistency (Linearizability)
*   **Description**: Subsequent reads always return the latest write.
*   **Mechanism**: Synchronous replication. A write must be committed to a quorum of nodes (e.g., Paxos, Raft) before returning success.
*   **Trade-off**: Higher latency, lower availability (if lead node dies or quorum is lost, writes fail).
*   **Examples**: RDBMS (ACID), etcd, ZooKeeper.

### 5.2 Eventual Consistency
*   **Description**: If no new updates are made, eventually all accesses will return the last updated value.
*   **Mechanism**: Asynchronous replication. The system returns success immediately after writing to one node; data propagates in the background (Gossip protocol).
*   **Trade-off**: Low latency, high availability, but chance of stale data.
*   **Examples**: DNS, Cassandra, DynamoDB (configurable).

### 5.3 Weak Consistency
*   **Description**: The system does not guarantee that subsequent accesses will return the updated value. Best effort.
*   **Examples**: Video streaming (dropping a frame is fine), real-time multiplayer game position updates, VoIP.

### 5.4 Client-Centric Consistency
These are specific guarantees for a single user's session:
*   **Read-Your-Writes**: After I utilize an updating operation, I will always see that update in my subsequent reads.
*   **Monotonic Reads**: If I realize a data value, I will never see an older version of that data in the future.

---

## 6. Availability Patterns

To achieve high availability (those 4 or 5 nines), we use architectural patterns.

### 6.1 Fail-over
Switching to a backup component when the primary fails.
*   **Active-Passive (Master-Slave)**: One node handles traffic; the other sits idle (or replicates logs) waiting to take over.
    *   *Pro*: Simple consistency reasoning (only one writer).
    *   *Con*: Waste of resources (passive node does nothing), failover time can cause downtime.
*   **Active-Active (Master-Master)**: Both nodes handle traffic.
    *   *Pro*: Full resource utilization, very high availability.
    *   *Con*: Complex data consistency handling (conflict resolution needed if both accept writes to the same key).

### 6.2 Replication
Copying data across multiple servers.
*   Reduces the "Sequential Component" risk; if one replica dies, others serve traffic.
*   Load Balancers are key here to health-check and route traffic only to healthy nodes.

### 6.3 Fail-over vs Replication
It's common to confuse these terms, but they address different layers of availability.

*   **Concept**: Fail-over is the *action* of switching control to a backup. Replication is the *mechanism* of ensuring data exists in multiple places.
*   **Relationship**: You can have replication without auto-failover (manual switch) and failover without replication (stateless app servers). However, in stateful systems, they usually work together.

#### Master-Slave Replication vs. Active-Passive Failover
*   **Master-Slave Replication**: Describes the **data flow**.
    *   One node (Master) accepts writes.
    *   Other nodes (Slaves) copy data from the Master.
*   **Active-Passive Failover**: Describes the **traffic flow**.
    *   One node (Active) handles all user traffic.
    *   The other node (Passive) sits idle or in standby mode, ready to take over if the Active node dies.
*   **Intersection**: This is the standard pattern for relational databases (e.g., PostgreSQL, MySQL). The "Master" is the "Active" node. If it dies, a "Slave" is promoted to be the new "Master" (Fail-over action).

#### Master-Master Replication vs. Active-Active Failover
*   **Master-Master Replication**: Describes the **data flow**.
    *   Writes can happen at *any* node.
    *   Nodes sync data bidirectionally. (Complex conflict resolution required).
*   **Active-Active Failover**: Describes the **traffic flow**.
    *   All nodes handle user traffic simultaneously.
    *   If one node dies, traffic is simply routed to the remaining nodes.
*   **Intersection**: This is common for high-scale global applications.
    *   *App Layer*: Stateless app servers are naturally Active-Active.
    *   *DB Layer*: Requires Master-Master replication (e.g., DynamoDB Global Tables, Cassandra) OR sharding to support true Active-Active behavior without a single point of failure.

---

## 7. Comparison Summary

| Feature | **Consistency (CP focus)** | **Availability (AP focus)** |
| :--- | :--- | :--- |
| **Primary Goal** | Data accuracy & integrity. | System uptime & responsiveness. |
| **Latency** | Higher (wait for sync). | Lower (return immediately). |
| **Partition Handling** | Reject requests or error out. | Serve stale data. |
| **Ideal For** | Banking, Inventory, Payments. | Social Feeds, Caching, Analytics. |
| **Algorithmic Cost** | Paxos, Raft, 2PC (Complex). | Gossip, Conflict Resolution (Complex). |

---

# References
* https://robertgreiner.com/cap-theorem-revisited
* https://www.youtube.com/watch?v=k-Yaq8AHlFA

*End of Document*
