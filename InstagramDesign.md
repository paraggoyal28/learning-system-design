# System Design of Instagram

## Functional Requirements
* Users can upload photos & videos
* Users can follow & Unfollow other users
* Users can view a scrollable feed of posts from people they follow 
(Chronological or Ranked)
* Users can search for users or hashtags

## Non-Functional Requirements
* High Availability
* Low Latency: Feed generation and image loading must be near instant (~ 200ms)
* Eventual Consistency: It's okay if a follower sees a post a few seconds late, but the 
data must never be lost
* Scalability: Support 1 billion users and millions of uploads per day

## High Level Architecture
Microservices architecture
* API Gateway: Handles authentication, rate limiting, and request routing
* User Service: Manages profiles and follower graphs (SQL/NoSQL)
* Media Service: Handles file uploads to Object Storage (S3) and metadata to a NoSQL DB
* Feed Service: Pre-computes and serves the user timeline

## Data Model and Storage

### Databases
* **Relational DB(PostgreSQL)**: Best for user profiles and follower relationships where ACID 
complaince is needed.
* **NoSQL DB (Cassandra/DynamoDB)**: Best for storing metadata for billions of posts 
(PostID, UserID, Lat/Long, S3 Url)
* **Object Storage (AWS S3/GCS)**: To store actual image/video files
* **Cache (Redis)**: For storing session data and pre-computed feeds

## Key Component Design

### A. Feed System (Fan-out Pattern)

Generating a feed for a user with 1,000 follows in real-time is computationally expensive. 

1. **Pull Model(for celebrity)**: For "Celebrity" accounts with millions of followers, we
don't push their posts to every follower cache. Instead, we fetch them when the follower 
opens the app

2. **Push Model(Fanout for write)**: For regular users, 