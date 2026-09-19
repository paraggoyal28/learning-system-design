# URL Shortener

Design a scalable URL shortener (such as TinyURL)

Step 1: High Level Requirements
## Functional Requirements
1. Given a long URL, the service should generate a short url.
2. When user gives the short url, the service should redirect to the long url.
3. Users should be able to optionally pick expiration time for each url.
4. Analytics: Tracking metrics as how many times a link is clicked.

# Non-Functional Requirements
1. System should be highly available  If service fails, all redirections will fail.
2. System should support low latency, the redirection should happen almost instantly.
3. Short links must not be guessable. (sequential ids should not return the total url counts).

# Capacity Estimation
Assume 500 Million URLs in monthly with 100:1 read to write ratio
500M/30*60*60*24 = 200 Urls/sec
Reads: 200Urls/sec * 100 = 20000 Urls/sec 

Storage (5 years):
Writes 500M/month * 12 * 5 = 30000M = 30 B urls.
Each url takes 500 bytes.
30B urls * 500 bytes = 15000*10^9 bytes ~ 15 10^12 bytes ~ 15 TB.

Step 2: High Level Design & API Endpoints

1. POST /api/v1/data
Parameter: long url (string), optional: expire_date (date)
Returns: Shortened url

2. GET /{short_url}
Parameter: short url (string)
Returns: HTTP Redirect (301 Permanently Moved or 302 Found) pointing to long_url

# High Level Architecture Components

1. Load Balancer (LB): Distributes incoming client traffic across application servers.
2. Web/Application Servers: Stateless servers that execute the shortening logic or fetch redirects.
3. Caching Layer: Speed up lookups by keeping hot, viral URLs in memory.
4. Database: Persists the mapping between short and long urls.

Step 3: Database Design

1. Relational VS NoSQL:
We have 30 billion records, but very simple access patterns: lookup short_url -> get long_url.
No joins.

2. A NoSQL key value store (like Cassandra or DynamoDB) or a sharded relational database works well. NoSQL is preferred here for seamless horizontal scaling and predictable low latency reads.

# Schema Design (NoSQL/key-value pair):

hash (Partition/Primary Key): String (eg. fx34a2) (only store the hash value) the base url 
can be something like www.example.com/
long_url: String
created_at: Timestamp
expiration_date: Timestamp


Step 4: Core Algorithms (The Shortening Logic)
How do we generate the short key ? 

Approach 1: Hash + Base62 Encoding
1. Take the long URL and pass it through a hashing algorithm like MD5 or SHA-256.
2. The MD5 hash yields a 128-bit hash value. If we put this in hex format (1 hex character = 4 bits), we get 32 hex characters. we choose hex characters that are (0-9A-F) because they are human readable, printable.
3. From the 32 characters, take first 6-8 characters, encode them in Base62 (a-zA-Z0-9), we get a short alphanumeric string.
4. The Problem: Hash Collisions. Two different long URLs could produce the same MD5 hash prefix. To resolve this, we would have to append a counter to the long URL until we find a non-existing hash, which degrades write performance.

Approach 2: Offline Key Generation Service (KGS) - Recommended Primer Pattern
To avoid collisions and heavy synchronization locks, we use a separate Key Generation Service
(KGS) that pre-allocates unique random or sequential strings.

* How KGS works
- A dedicated service keeps track of a unique 64-bit integer counter or random generator.
* It generates unique strings of length 6 using Base62 conversion ahead of time and stores
them in a database table of "unused keys".
* When an application server needs a key, it requests it from KGS. KGS marks that key as "used" 
and hands it to the app server.
* This completely eliminates runtime collisions and makes writes fast.

Step 5: Scaling the design (Deep Dive)
1. Caching (Addressing Read Bottlenecks)
- Because of 20,000 QPS read load, hitting the database for every single click would overwhelm it.
- We introduce a Distributed Cache (e.g Redis or Memcached) in front of database.
- Caching Strategy: Whenever a user requests a short URL, the application server checks Redis first.
* Cache Hit: Returns the long URL immediately (~1ms)
* Cache Miss: Queries the database, writes the result back to Redis, and returns it.
- Eviction Policy: Use LRU (Least Recently Used) so that viral, frequently clicked links stay cached. To cache 20% of daily reads (Pareto Principle), the memory footprint is modest and easily clusterable.

The Pareto Principle (80/20 rule) in Web Traffic
In systems like a URL shortener or social media platform, traffic is never distributed evenly.
A small percentage of URLs (like a newly viral news article, a trending product link, or a celebrity's tweet) get clicked million of times.
Meanwhile, the vast majority of created links are rarely clicked after their first day.
According to 80/20 rule applied to caching: 20% of the active URLs generate 80% of the read (redirectional) traffic.

Let's calculate memory footprint: 
Modest memory footprint: Lets calculate it. Say we have 20,000 reads per second (QPS), that translates to roughly 1.7 million read requests per day. If 20% of those represent unique active 
hot links, we only need to store about 340,000 records. 340,000 unique URLs in cache at any given time. At ~500 bytes per record, 340,000 * 500 = 170000000 bytes = 1.7 * 10^8 ~ 170 * 10^6 bytes ~ 170MB to 200MB of RAM - which is tiny by modern server standards.

Easily Clusterable
Because the memory requirement is so small (or even if it scales up to a few GBs), we don't need a single monolithic supercomputer. We can distribute(cluster) the cache across multiple cheap, lightweight Redis or Memcached nodes using consistent hashing, allowing the system to scale horizontally with zero hustle.


2. Database Scaling (Sharding & Replication): 
15TB over 5 years is manageable, but 20k+ read QPS requires scaling.
Read Replicas: Route all read redirection queries across multiple database read replicas.
Sharding: If storage or write throughput scales beyond a single database node, partition data by
hash key using Consistent Hashing to evenly distribute rows across database clusters.

3. Load Balancing & Geographic Distribution
Place Layer 7 Load Balancers (like Nginx or AWS ALB) to handle SSL termination and route traffic.
For global users, use Geo-DNS to route a user's request to the closest regional data center, 
drastically minimizing network round-trip latency.

4. HTTP 301 vs 302 Redirect Trade-off
301 Permanent Redirect: The browser caches the redirection permanently. Subsequent clicks skip
our servers entirely. This reduces server load drastically, but breaks analytics since click
requests never hit our backend.

302 Temporary Redirect: The browser don't cache it. Subsequent clicks hit our backend servers,
allowing us to accurately track metrics (click counts, referral traffic, device type). at the expense of higher server load.

Interview conclusion: Use 302 if analytics are a core product requirement; use 301 if raw 
throughput and minimum server overhead are desired.

Step 6: Cleaning Up & Security (Bonus Points!)
Lazy Deletion/Cleanup Worker: Instead of letting expired links clutter storage. run a 
background cron worker during off-peak hours to clean up expired rows, or handle deletion lazily
upon access checks.

Rate Limiting: Protect the POST creation endpoint against abuse (eg. malicious scripts scamming 
millions of links) by implementing a Token Bucket rate limiter per IP address using Redis.









