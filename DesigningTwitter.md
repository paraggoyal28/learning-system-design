# Designing Twitter (Real-Time News Feed)

## Requirements

### Functional Requirements
* Post Tweet: Users can post text messages (max 280 chars) with optional media (images/videos)
* Timeline (Home Feed): Users can view an aggregated feed of tweets from people they follow (reverse chronological)
* Follow/Unfollow: Users can follow other users
* User Timeline: Users can view their own or another specific user's past tweets

### Non-Functional Requirements
* High Availability: The system must be available 99.99% of the time (CAP Theorem: AP over CP)
* Low Latency: Feed generation must be under 200ms
* Read-Heavy: The system will have high read:write ratio (100:1)
* Eventual Consistency: It is acceptable if a follower sees a tweet a few seconds after it is posted

## Back of Envelope Calculation

* **DAU (Daily Active Users)**: 200 Million
* **Tweet Rate**: Average 2 tweets/day per user -> 400M tweets/day
* **Read Rate**: Average 20 reads/day per user -> 4B timelines read/day
* **QPS (Writes)**: 400M/86400 ~ 4600 tweets/sec (Peak ~ 10K/sec)
* **QPS (Reads)**: 4B/86400 ~ 46000 reads/sec (Peak ~100K/sec)
* **Storage**: 400M tweets * 300 bytes (metadata + text) ~ 120 GB/day. Media storage is separate (PB scale)
Insight: This challenge is not storing tweets (120GB/day), but **fan-out** (distributing 1 tweet to 10M followers) and **read latency**

## High Level Architecture 
Move from monolithic to microservices

### Core Services:
* **Tweet Service**: Handles posting and storage of tweets
* **User Graph Service**: Manages user metadata and follow relationships
* **Timeline Service(Fan-out)**: Responsbile for constructing the Home Feed
* **Asset Service**: Handles media updates (S3 + CDN)

## Data Model & Schema

### Database Choices
* **User Service(SQL)**: MySQL/PostgreSQL. User data is relational and structured. Strong consistency is needed for account data
* **Tweet Service(NoSQL)**: Cassandra/DynamoDB Why ? High write throughput, easy horizontal scaling (Sharding), flexible schema 
* **User Graph(Graph DB or SQL)**: Many designs use SQL with an association table (follower_id, followee_id) efficiently indexed. Complex graph queries (friends of friends) are rare here; as it's mostly like "Who follows User A?"

### Schema Definition
#### Tweets Table (Cassandra)
* Partition Key: user_id (Keeps a user's tweets on the same shard for fast retrieval of user timeline)
* Clustering Key: timestamp (Sorts tweets chronologically)
* Columns: tweet_id, content, media_url, created_at

#### Follows Table (MySQL): 
* Primary Key: (follower_id, followee_id)
* Indexed on followee_id to quickly answer: "Who needs to see this new tweet ? "

## Deep Dive: Timeline Generation (The Core Challenge)

### Approach 1: Pull Model (Fan-out on Read)
When User A requests their feed, the system: 
1. Fetches IDs of everyone User A follows (index on followee_id helps here)
2. Queries the Tweet Service to get the recent tweets of all those IDs
3. Merges and sorts them in memory
4. **Pros**: Simple write path. No storage overhead for feeds
5. **Cons Terrible Read Latency**: If User A follows 1000 people, we execute 1000 DB queries (or 1 large multi-get) every time
they refresh the page

### Approach 2: Push Model (Fan-out on Write)
When UserB posts a tweet:
1. System fetches all of UserB's followers.
2. System writes the Tweet ID into a pre-computed "Timeline List" (in Redis) for each follower
3. When UserA reads their feed, they just read their own Redis list. O(1) for read
**Pros**: Lightning fast reads
**Cons Write Amplification**: If a user has 1M followers, 1 tweet triggers 1M writes to Redis.

### The Solution: Hybrid Approach
We use **Push** for normal users and **Pull** for celebrities (Hot Users)
1. **Normal User Tweets (e.g. <10k followers)**: Use **Push**. Fan-out the tweet ID to all follower caches immediately
2. **Celebrity Tweets (e.g. >10K followers)**: Use **Pull**. Save the tweet to the DB only. Donot fan out
3. **Feed Retrieval**: When a user requests their feed, the system fetches:
* Their pre-computed feed from Redis (Normal Users)
* Queries the specific "Celebrity" tweets they follow (Pull)
* Merged them at runtime
This balances write load (no massive fan-out storms) and read-latency

## Detailed Component Design

### Fanout Service
This service is a worker cluster (processing queues)
1. **Ingestion** - User post tweets -> Persisted to Cassandra -> Event published to Kafka (NewTweetEvent)
2. **Processing** - Fan-out workers consume from Kafka
3. **Differentiation** - 
* Worker checks UserGraph - Is this user a celebrity ? 
* No - Retrieve all follower IDs. Pipeline writes to Redis clusters for each follower.
* Yes - Do nothing

### Cache Architecture (Redis)
We cannot store the entire timeline in Redis for everyone
* **Limit**: Store only the last 800 Tweet IDs per user in Redis
* **Data**: Store {tweet_id, user_id, content_preview, timestamp} 
* **Mis**: If a user scrolls past the 800th tweet, we fall back to DB (Pull Model) for older data

## Scaling & Partitioning

### Sharding the Tweet Database
* **Sharding by User Id**
    * *Pros*: All tweets for a user are on one node. Fast "User Timeline" queries.
    * *Cons* **Hot User Problem**: If Elon must tweets, that shard will get hammered.
* **Sharding by Tweet Id(Snowflake ID)**
    * *Pros*: Even distribution of write load.
    * *Cons*: "User Timeline" requires querying all the shards (Scatter-gather), which is slow
* **Decision: Shard By User ID** but handle hot reads via aggressive caching

### ID Generation (Snowflake)

We need a unique, sortable 64-bit ID. We cannot use auto-increment (databases are sharded)
* **Structure**: [Timestamp (41 bits)] + [Machine ID(10 bits)] + [Sequence (12 bits)]
* Allows sorting by ID to equal sorting by time (roughly)

## Failure Scenarios & Bottlenecks

### Cache Failure
* **Scenario**: Redis cluster goes down.
* **Impact**: Read latency spikes as requests fall back to DB (Pull Request)
* **Mitigation**: **Replication** (Redis Sentinel) for high availability. Do not rebuild cache from DB immediately
(thundering herd); instead, rebuild gradually or serve a degraded "recent only" timeline

### The "Bieber" Problem (Celebrity Write Spike)
* **Scenario**: Justin Beiber tweets. 100M followers need updates
* **Mitigation**: As discussed in the Hybrid section, we disable fan-out for him. His followers pull his tweet only when
they open the app

### Lag In Fan-Out
* **Scenario**: Kafka backlog grows; users don't see their friend's tweet for 5 minutes
* **Mitigation**: Auto-scale worker consumers based on lag metrics. Prioritize "live" users (users currently online)
over inactive users

## API Design (Sample)

### Post Tweet 
POST /v1/tweet

{
  "user_id": "uuid",
  "content": "Hello World",
  "media_ids": ["uuid_1"]
}

Get Home Timeline: GET /v1/timeline/home?cursor=12345&limit=20
* Returns a list of Tweet Objects
* Uses cursor based pagination (using TweetID) rather than Offset (which is slow on large datasets)

## Summary for the interviewee
* **System is read-heavy**: Optimize using Redis Cache and pre-computed timelines.
* **Latency Vs Consistency**: We chose eventual consistency to ensure high availability and low latency
* **Scalability**: Solved via Hybrid Push/Pull Model and UserID sharding
* **Storage**: Tweet text in Cassandra (scalable), Relationships in MySQL (relational integrity), Media in S3 (Object storage)


