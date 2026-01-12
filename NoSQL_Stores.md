# NoSQL Databases

## Key-Value Stores
The **Distributed Hash Table** data is stored as a unique identifier (key) and a pointer to a
data blob (value). The database is generally "opaque" to the value, meaning it doesn't care
what is inside.
* **Architectural Focus**: Extreme low latency and high throughput
* **Best For**: Caching, session management, and real time leaderboards
* **Consistency Model**: AP though some (Redis) can be configured for strong consistency
Examples: Redis, Amazon DynamoDB, Riak
**SDE2 Tip**: Use this when your access pattern is purely based on a unique ID. 
If you find yourself needed to filter by an internal attribute (e.g "find all sessions
for User X"), a Key-Value store will force a full scan or require you to maintain a 
secondary index yourself.

## Document Stores
The **JSON-First Database** Data is stored in semi-structured formats like JSON, BSON, or
XML. Unlike Key-Value Stores, the database *understands* the document structure, allowing 
you to index and query internal fields.

* **Architectural Focus**: Schema flexibility and developer productivity
* **Best For**: Content management systems, e-commerce catalogs (where products have
varying attributes), and user profiles
* **Consistency model**: Often CP (Consistency and Partition Tolerance), but many offer
"Tunable Consistency" (MongoDB)
* Examples: MongoDB, CouchBase, Amazon DocumentDB

## Wide Column (Column Family) Stores
The **Big Data Warehouse** instead of storing data in rows, these stores group data into 
"column families". This allows for very fast writes and efficient storage of sparse data
(where many rows might have null values)

* **Architectural Focus**: Massive horizontal scalability (Petabytes), and high write 
availability
* **Best For**: Log data, IoT telemetry, time-series data, and large-scale analytics where
you frequently aggregate values from a single column
* **Consistency Model**: Typically AP (Eventual Consistency), emphasizing that the system
may stay up even if some nodes are out of sync
* **Examples: Apache Cassandra, HBase, ScyllaDB**
SDE2 Tip: Unlike SQL, wide column stores require **Query-Driven Modeling**. You must know your 
queries before you design your tables. Joins are *non-existent*; you denormalize your data into 
the exact shape your UI needs

## Graph Database 
The **Relationship First** Engine Data is stored as Nodes (entites) and Edges (relationships)
The relationship is a first class citizen, meaning the "link" between two people is just as easy to query as the people themselves.

* **Architectural Focus**: Complex relationship traversal without the performance "Join-bomb" of SQL
* **Best For**: Social networks, recommendation engines, fraud detection (identifying clusters 
of suspicious accounts), and Knowledge Graphs.
* **Consistency Model**: ACID complaint on a single node (like Neo4j), but scaling across a 
distributed cluster is more complex than other NoSQL types.
* Examples: Neo4j, Amazon Neptune, ArangoDB

|Feature|Key-Value|Document|Wide-Column|Graph|
|----|----|----|---|---|
|Primary Goal|Latency|Flexibility|Scalability|Relationships|
|Schema|None|Flexible/Dynamic|Semi-structured|Flexible|
|Queryability|Key only|Rich (Fields/Indexes)|Partition/Row Key|Graph Traversal (Cypher/Gremlin)|
|Scaling|Horizontal|Horizontal|Massive Horizontal|Vertical (mostly)|

**SDE 2 Decision Guide: "Which one do I pick?"**
Do you need sub-millisecond response for simple lookups? → Key-Value (Redis).
Does your data structure change every week or have nested objects? → Document (MongoDB).
Are you writing millions of logs per second and need to store them for years? → Wide-Column (Cassandra).
Is your most frequent query "Find friends of friends who like X"? → Graph (Neo4j).


