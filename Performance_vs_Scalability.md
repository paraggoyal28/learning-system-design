# Performance vs. Scalability & Latency vs. Throughput

**Target Audience:** Software Engineers transitioning from SDE 1 to SDE 2.

As you grow into a Senior Software Engineer role, your focus shifts from "writing working code" to "designing robust systems." Two pairs of concepts are fundamental to system design and often confused: **Performance vs. Scalability** and **Latency vs. Throughput**. Understanding the nuance between these is critical for making architectural trade-offs.

---

## Part 1: Performance vs. Scalability

In short: **Performance is about "fast." Scalability is about "growth."**

### 1. Performance
**Definition:** Performance measures how efficiently a system handles a *single* unit of work. It is often synonymous with response time or service time for one user.
*   **Goal:** Minimize the time taken to process a request.
*   **Optimization:** Optimizing algorithms, reducing database queries, using caching, or code profiling.
*   **SDE 1 Focus:** "Make this function run in O(n) instead of O(n^2)."

### 2. Scalability
**Definition:** Scalability measures how well a system handles an *increasing* amount of work by adding resources. It is the ability to maintain performance definitions as load increases.
*   **Goal:** Maintain acceptable performance levels as traffic (or data) grows.
*   **Optimization:** Adding load balancers, sharding databases, moving to microservices, or decoupling components.
*   **SDE 2 Focus:** "Ensure this service doesn't crash if traffic spikes by 10x."

### The Analogy: The Car vs. The Highway
*   **Performance** is like a Ferrari. It can go from point A to point B extremely fast. But if you have 100 people to transport, a single Ferrari is not a scalable solution.
*   **Scalability** is like a Highway. Adding more lanes (resources) allows more cars (requests) to travel simultaneously without slowing down. A bus on a highway might be slower (lower performance) than a Ferrari, but a fleet of buses on a wide highway scales better for mass transit.

### Comparison Table

| Feature | Performance | Scalability |
| :--- | :--- | :--- |
| **Core Question** | "How fast is it for one user?" | "How much load can it handle?" |
| **Problem Solved** | Slowness / Latency. | Bottlenecks / Capacity limits. |
| **Key Metric** | Response time (ms), CEO execution time. | Max Concurrent Users, RPS (Requests Per Second). |
| **Solution** | Tune code, optimize queries, upgrade hardware (Vertical Scaling). | Add servers, distributed systems, partitioning (Horizontal Scaling). |
| **Context** | Single Thread / Single Request. | Entire System / Distributed Architecture. |

> **Critical Insight for SDE 2:** A highly performant system is not necessarily scalable (e.g., a super-optimized single-threaded app). A highly scalable system might not be individually performant (e.g., a distributed system with network overhead). Your job is to balance both.

---

## Part 2: Latency vs. Throughput

In short: **Latency is "time." Throughput is "volume."**

### 1. Latency
**Definition:** The duration between making a request and receiving a response.
*   **Units:** Milliseconds (ms), Seconds (s).
*   **User Perspective:** "It feels slow."
*   **Components:** Network propagation + Processing time + Queueing delay.

### 2. Throughput
**Definition:** The number of items (requests, bits, transactions) processed per unit of time.
*   **Units:** Requests Per Second (RPS), Transactions Per Second (TPS), Bits Per Second (bps).
*   **System Perspective:** "How many users can we serve?"

### The Analogy: The Water Pipe
*   **Latency** is the length of the pipe. It determines how long it takes for a single drop of water to travel from source to destination.
*   **Throughput** is the width (diameter) of the pipe. A wider pipe allows more water to flow through at once, even if the travel time (latency) remains the same.

### The Relationship: Little's Law
In a stable system, there is a mathematical relationship between these metrics and concurrency:
$$ L = \lambda \times W $$
*   **L** = Average number of items in the system (Concurrency).
*   **$\lambda$** = Average arrival rate (Throughput).
*   **W** = Average time an item spends in the system (Latency).

*Practical Implication:* If you want to handle more Throughput ($\lambda$) without increasing Latency ($W$), you must increase the system's capacity ($L$)—usually by adding more servers (Scaling).

### Trade-offs: The "Batching" Example
A classic system design interview question involves **Batching**.
*   **Scenario:** You group 50 database writes into a single batch request to save network overhead.
*   **Effect on Throughput:** **Increases**. You process more records with fewer connections.
*   **Effect on Latency:** **Worsens**. The first record in the batch has to wait for 49 others to arrive before it gets sent.
*   **Decision:** Is the system latency-sensitive (Online Gaming) or throughput-sensitive (Log Processing)?

### Comparison Table

| Feature | Latency | Throughput |
| :--- | :--- | :--- |
| **Focus** | Speed of a single task. | Volume of tasks over time. |
| **Ideal State** | Low (Near 0). | High (Maximized). |
| **Limiting Factor** | Distance (speed of light), Algorithm efficiency. | Bandwidth, CPU Cores, Memory. |
| **Optimization** | CDNs, Edge Computing, Caching. | Parallelism, Async processing, Batching. |

---

## Summary for SDE 2

*   **SDE 1** typically optimizes for **Performance** and **Latency** (making the code fast).
*   **SDE 2** must also design for **Scalability** and **Throughput** (making the system robust).

When designing a system, always ask: **"Are we optimizing for the first user (Latency/Perf) or the millionth user (Throughput/Scale)?"** usually, you need to compromise on one to get the other.

References:
* https://www.allthingsdistributed.com/2006/03/a_word_on_scalability.html
* https://www.slideshare.net/slideshow/scalability-availability-stability-patterns/4062682
* https://community.cadence.com/cadence_blogs_8/b/fv/posts/understanding-latency-vs-throughput