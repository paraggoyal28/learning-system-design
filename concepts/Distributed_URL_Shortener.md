# Distributed URL Shortener

## Requirements

### Functional Requirements

1. **Shortening**: Given a long URL (or text blob), generate a unique, short alias (e.g,
bit.ly/AbC12)
2. **Redirection/Retrieval**: Accessing the short alias should redirect to the original URL or 
display the text content
3. **Expiration**: Users can specify an expiration time (TTL). Default TTL applies if unspecified.
4. **Custom Alias**: Users can request a custom short key. (e.g bit.ly/my-blog)
5. **Analytics**: Track click stats (timestamp, location, referrer) for the link owner

### Non Functional Requirements

1. **High Availability**: System must be always available for reads (redirects). 
Availability > Consistency (AP system).
2. **Low Latency**: Redirection should happen in < 20ms (excluding network latency).
3. **Scalability**: Must handle read-heavy traffic (eg. 100 reads/writes ratio)
4. **Uniqueness**: Short keys must be strictly unique
5. **Durability**: Once a URL is shortened, it should persist until expiration.

## Capacity Planning (Back of the Envelope Calculation)

* **Traffic**: 100M new URLs/month
* **Read/Write Ratio**: 100:1
    * Writes: 100M/30 days ~ 3M/day ~ 40 writes/sec
    * Reads: 40 * 100 = 4000 reads/sec
    * Peak-traffic: 5x average ~ 200 writes/sec ~ 20K reads/sec
* **Storage**:
    * URL Entry Size: ~500 Bytes (ID, OriginalURL, CreatedAt, UserID, etc)
    * Storage/Month: 100M * 500B ~ 50GB
    * 5 year storage: 50GB * 5 * 12 ~ 3000 GB ~ 3TB
    * Pastebin Note: If storing text blobs (avg 10KB) ~ storage scales to 60TB over 5 years
* **Memory(Cache)**: 
    Follow the 80/20 rule (20% of the URLs generate 80% of the traffic)
    * Daily Requests: 4000 * 86400 ~ 350M req/day
    * Unique URLS accessed: Assume 20% are unique
    * Cache Memory: 350M * 20% * 500B ~ 35GB. A standard 64GB Redis instance suffices.

## API Design (REST)
A. Create Short URL: POST /api/v1/urls
// Request
{
  "long_url": "https://very-long-website.com/resource/id/123",
  "custom_alias": "optional-custom-string",
  "expires_at": "2026-12-31T23:59:00Z"
}

// Response (201 Created)
{
  "short_url": "https://bit.ly/AbC12",
  "short_key": "AbC12"
}

B. Get/Redirect URL Get /{short_key}
* Response: HTTP 301 (Permanent) or 302 (Temporary Redirect)
* SDE2 TradeOff: Use **301** to reduce server load (browser caches the redirect), but use
**302** if you strictly need analytics for every click

## Database Schema

Choice: NoSQL (DynamoDB/Cassandra) or Sharded SQL. Why NoSQL ? Scale is massive, 
relationships are simple (key-Value), and we need high write throughput

Table: **URL_MAPPING**
|Column|Type|Description|
|----|----|----|
|short_key (PK)|VARCHAR(7)|The unique 7-char alias (Partition Key)|
|long_url|VARCHAR|The original URL|
|created_at|TIMESTAMP|Creation time|
|expires_at|TIMESTAMP|TTL for cleanup|
|user_id|VARCHAR|Owner ID (for analytics/management)|

Table: **User**
Standard user table (UserID, Name, Email, APIKey)

## High Level Architecture

1. **Client** sends request
2. **Load Balancer** distributes traffic across application servers.
3. **Web Servers** are stateless services handling API logic
4. **Cache (Redis)**: Stores short_key -> long_url. Hits return immediately
5. **Database** Persistent storage for mappings
6. **Key Generation Service (KGS)**: Asynchronously pre-generate unique keys to avoid 
collisions on insert.

## Deep Dive: Unique ID Generation

How do we generate a unique 7 character string efficiently ? 

**Approach A: Online Hashing (MD5/SHA256)**
Hash the *long url* + *timestamp* (to ensure uniqueness for same URL)
* MD5(url + timestamp) = 128 bit hash
* Base62 Encode the hash
* Take the first 7 characters
* **Trade-off**: Potential collisions. Requires a "Check if exists then retry" loop, 
which increases write latency

**Approach B: Distributed unique ID Generation (Snowflake)**
Use a distinct ID generator (like Twitter Snowflake) to generate unique 64 bit integers, then 
Base62 encode them
* **Pros**: fast, sorted by time, distributed
* **Cons**: Requires running a separate Zookeeper/coordination service

**Approach C: Pre-Generated Keys (Key Generation Service - KGS) Recommended**
We decouple the key generation from the user request path
1. **Offline Generation**: A standalone service generates random 7-char strings (Base62) and
stores them in a *KeyDB*
2. **Two tables in KeyDB**: 
    * *Unused_Keys*: Generated keys ready to be used
    * *Used_Keys*: Keys currently assigned to URLs
3. **Workflow**:
    * App server requests a batch of keys (eg. 1000) from KGS.
    * KGS moves these keys from *Unused* to *Used* in the memory/DB
    * App Server buffers keys locally. When a user requests a short URL, the server grabs a 
    key from its buffer.
* Pros: zero collision checks at runtime. Extremely fast.
* Cons: If an App Server crashes, the buffered keys are "lost" (acceptable waste)
**SDE2 Insight**: KGS is a single point of failure (SPOF). To fix this, use a primary replica
setup for KGS or Zookeeper to manage range of keys

## Pastebin Specifics (Handling text blobs)

For a system like Pastebin, the architecture changes slightly. We do not store the text content 
in the *URL Mapping* table (DBs are bad for large Blobs)
1. **Object Storage (S3)**: Stores the actual text content here.
- Filename=short_key
2. **Database**: Stores metadata only
* short_key -> s3_link, created_at, user_id
3. **Flow**: 
    * Write: App Server uploads text to S3 -> Gets S3 URL -> Saves metadata to DB -> 
        Returns short_key
    * Read: App Server looks up DB -> Gets S3 Path -> Fetches content (or redirects CDN to S3) -> Returns text

## Advanced Scaling & Optimizations

### Database Partitioning (Sharding)
To store billions of rows, we must shard
* **Range-based Sharding**: Based on the first character of the hash (eg. 'A', 'B'). Issue: 
Unbalanced partitions (some prefixes are more common)
* **Hash-based Sharding**: Hash(short_key)%N (where N is the number of DB nodes).
*Benefit*: Uniform Distribution

### Caching Strategy
* **Eviction Policy: LRU (Least Recently Used)**: We want to keep hot links (viral posts) in 
memory
* **Cache Invalidation**: 
    * Since URLs rarely change, valid for long TTL (e.g 24 hours)
    * If a user updates a custom alias (rare), use **Write-Through** cache

### Handling Hot Keys
If a celebrity posts a link, that short_key will be read by many users so it will overwhelm one 
partition
* **Solution**: Aggressive caching. For extremely hot keys, replicate the read across multiple 
cache nodes or use a **CDN** to serve the redirect/content at the edge.

### Analytics (Asynchronous)
Do **not** write analytics synchronously to the DB during the redirect (adds latency)
* **Solution**: Send an event to a Message Queue (Kafka) -> ('short_key', 'timestamp', 'ip', 'user_agent')
Here the ip refers to the Client's IP Address.

1. Why capture the Client IP ? 
The primary purpose of capturing the client IP in an analytical system is **Geo-location
lookup**
* Raw data: 203.0.113.40
* Derived data: When the worker processes this event from kafka, it looks up the IP in a 
GeoIP database (like MaxMind) to derive: Country (India), City (Bengaluru), ISP (Airtel)
* End Goal: To show the link creater a dashboard saying: "80% of your clicks came from
India"

2. Implementation Detail (Getting the Real IP)
Since your architectural diagram places the Web Servers behind a Load Balancer, simply reading
the source IP of the incoming packet will give you the Load Balancer IP, not the user's
We must read the HTTP headers injected by the LB:
* *X-Forwarded-For*: This header contains a comma separated list of IPs. The first IP in the 
list is the original client's IP.
* Example Header: X-Forwarded-For: 203.0.113.40, 10.0.0.1 (Client IP, LB IP).

3. Privacy & Complaince 
Storing raw IP addresses carries significant risk (GDPR/CCPA complaince). A production-ready 
design should include an anonymization step
1. **Ingest**: Capture IP 203.0.113.40
2. **Process**: Convert IP to "Bengaluru,IN"
3. **Store**: Save the location, but discard or hash the raw IP address to protect user
privacy

* **Workers**: Consume Kafka stream and aggregate data into an OLAP DB (e.g ClickHouse or 
Hadoop).

### Data Cleanup
Scanning the DB to delete expired keys is expensive
**Lazy cleanup**: When a user tries to access an expired link, return 404 and delete it then
**Scheduled Job**: A low priority batch job runs daily during off peak hours to remove 
expired keys from the DB and *Used Keys* table

### Final System Flow Summary

1. **Write Path**
* Client POST /shorten
* Server grabs pre-generated key from local buffer
* Server writes mapping to DB (and S3 if pastebin)
* Server returns short_key

2. **Read Path**
* Client GET /AbCI2
* LB routes to Server
* Server checks **Redis Cache**
    * Hit: Returns 301 Redirect
    * Miss: Fetch from DB, updates cache, Returns 301 Redirect
* Async: Fire event to Kafka for analytics.












