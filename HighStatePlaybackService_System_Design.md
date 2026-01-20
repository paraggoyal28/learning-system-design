# High State Playback Service System Design

This document details the architecture for a high scale Playback State Service 
(Resume/Pause), modeled after systems like Netflix's **RENO** and **Viewing History** service

## Core Objectives & Constraints

Goal is to manage a **high velocity, write-heavy traffic** while maintaining a **low-latency reads** for a seamless "Continue Watching" experience
* **Write-to-Read Ratio**: 10:1 (Updates happen every few seconds during playbacks; reads happen only at start-up)
* **Availability**: Must be highly available. A failure in this service should not block video
playback (fallback to start of video)
* **Consistency**: Eventual consistency. "Read-Your-Writes" preferred for active device.

## High Level Architecture

The system follows an event-driven, asynchronous pipeline to prevent the write-heavy heartbeats from bottlenecking the playback experience.

**A. Ingestion Tier (Client to Kafka)**
Clients (TV, Mobile, Web) don't send a "Save" request only at pause. They send **Heartbeats** every 10-15 seconds.
**Payload Example**
{
  "profile_id": "u123",
  "movie_id": "m456",
  "video_timestamp_ms": 540500,
  "client_request_time": 1705820000,
  "device_id": "phone_01",
  "event_type": "HEARTBEAT" // or PAUSE, STOP, SEEK
}

**API Gateway (Zuul/AppSync)**: Validates the token and routes the heartbeat to a high-throughput stream (e.g **Apache Kafka** or **AWS kinesis**)

**B. The Processing Tier (Stream Processing)**
A stream processing engine (like **Apache Flink** or **Manhattan**) consumes from Kafka.
* **Deduping**: If multiple heartbeats for the same *profile_id* + *movie_id* arrive in a short 
window, the processor only pushes the latest one to the database
* **Validation**: It ensures *video_timestamp* does not exceed the movie duration

## Storage Strategy: The Polyglot Approach

Netflix splits viewing history into two distinct layers to optimize cost and performance.

*A. Hot Layer: Redis (Session State)*
1. **Role**: Stores the *most recent* playback position for active profiles
2. **TTL**: Keys typically expire after 24-48 hours of inactivity
3. **Key Pattern**: v1:playstats:{profile_id}:{movie_id}

*B. Persistent Layer: Cassandra (Source of Truth)*
Cassandra is chosen for its linear scalability and high write throughput

**Data Modeling**:
* **Partition Key**: *profile_id* (Ensures all history of one user is on the same physical node)
* **Clustering Key**: *movie_id* or *timestamp* (To allow sorted retrieval of "Recently Watched")

**Storage Optimization (LiveVH vs CompressedVH)**: 
*LiveVH*: Stores recent, uncompressed data for the "Continue Watching" row
*CompressedVH*: As history grows, older data is compressed into a single BLOB per user to save 
storage costs

## Solving Distributed System Challenges

### A. Out-of-order Events (LWW with Wall-Clock)
In a mobile environment, Heartbeat #2 (at 01:20) might arrive before Heartbeat #1 (at 01:10) 
due to network jitter

* **Solution**: The system uses *client_request_time* (Wall-clock) for **Last Write Wins(LWW)**
resolution. The database only updates the record if the incoming *client_request_time* is greater than the currently stored one.

### B. Conflict Resolution (Multi-Device)
If a user watches the same movie on an iPad and a TV simultaneously.
* **Max Timestamp Rule**: Many systems choose the largest *video_timestamp_ms* to ensure the 
user doesn't lose progress, though some implementations stick to the "Last Activity" based on
*client_request_time*

### C. Cross-Device Sync (Rush Mechanism)
When you pause on your phone, the TV UI should ideally update immediately
* **Sync Service(RENO)**: After the database update, an event is published to a 
"Notification Topic".
* **WebSockets**: If the TV has an active Websocket connection to the gateway, the server
**pushes** a silent notification. "update state: {movie_id: 456, pos: 540500}"

## Performance Optimizations

1. **Write Batching**
Technique: *Client-side buffering*
Why ? Instead of 1 request per 10s, batch 3 heartbeats and send every 30s if the app remains
in the foreground

2. **Read Repair**
Technique: *Lazy Sync*
Why ? On a "cache miss" in Redis, the system fetches from Cassandra and re-populates Redis
immediately

3. **Delta Encoding**
Technique: *Storage*
Why ? Instead of storing the full state, store only the "Change" if the user is just watching linearly

## Failure Modes & Mitigations

1. **Database Outage**: If Cassandra is down, the service falls back to the Redis cache. If both are down, the client defaults to *timestamp: 0*

2. **Network Partition**: During a "Split Brain" scenario, regions operate independently. When 
the partition heals, the record with the latest *client_request_time* becomes the global truth.

3. **Hot Partitions**: If a specific *profile_id* is hammered (e.g a test account), the system
uses **Rate Limiting** at the Gateway level to drop excess heartbeats.

## References:
https://www.youtube.com/watch?v=n_SXhW-x0WA









