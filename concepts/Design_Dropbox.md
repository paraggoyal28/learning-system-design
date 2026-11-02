# Design Dropbox

## Requirements Clarification

### Functional Requirements

* **Upload/Download**: Users can upload and download files
* **File Sync**: Files must synchronize automatically across multiple devices and users
* **File History/Versioning**: Support for tracking changes and reverting to previous versions
* **Out Of Scope**: Real-time collaboration (like Google Docs live typing) is explicitly excluded for this design

### Non-Functional Requirements

* **High Reliability/Durability**: Data cannot be lost
* **High Scalability**: Must support millions of users and petabytes of data
* **Consistency**: Metadata must be consistent across clients

## Back of the envelope estimation

* **Users**: 500 Million DAU
* **Usage**: 1 file upload/user/day
* **Avg File Size**: 150 KB
* **Daily Storage**: 500M * 150KB ~ 75TB/day
* **Yearly Storage**: 75TB/day * 365 days ~ 27PB
* **10-Year Storage**: 270 PB
* **Throughput(QPS)** ~ 6000 writes/sec

## Core Design Decision: Block Level Storage

**Why not send the whole file ?** 
If a user edits 1 line in a 2GB file, uploading the entire 2GB file consumes massive bandwidth
and storage
* **Solution: Chunking (Block level Storage)**: Break files into smaller blocks (e.g 4 MB)
* **Benefit**: Only modified blocks are transferred (Delta Sync). This drastically reduces 
bandwidth and latency

## High Level Architecture
The system is divided into client side and server side components

**A. Client Component**
The client must do heavy lifting to enable offline capability and delta sync
1. **Monitor**: Runs in the background, watching the local workspace for file changes
2. **Blockify Service**: Splits files into blocks. Also handles **Compression** (e.g Huffman) and **Encryption** (AES 256) before upload
3. **Local Database**: Stores metadata locally (e.g SQLite) to track the state of files without
contacting the server
4. **Synchronizer**: The orchestrator. It communicates with the backend services to sync changes

**B. Backend Services**
1. **Block Service (Content Storage)**
    * Responsible for storing the actual raw data blocks (chunks)
    * Uses cloud object storage (e.g AWS S3) for durability
    * **Optimization**: Implements **Deduplication**. Before uploading, the client sends a hash of the block. If the server already has a block with that hash (from another user), it doesn't upload it again.

2. **Metadata Service** 
    * Manages file metadata (names, hierarchy, permissions, block lists)
    * **Database**: A relational database (MySQL/PostgreSQL) is preferred over NoSQL here to 
    ensure **ACID properties and consistency** of the file structure.
    * **Cache**: Use Redis/Memcached for frequently accessed metadata (hot files)

3. **Notification Service**
    * Crucial for keeping clients in sync. When a file updates, other clients need to know 
    immediately
    * **Protocol Choice: Long Polling** is preferred over Websockets
        * **Why?** Websockets provide bi-directional communication (chat apps), but here 
        communication is mostly one way (Server->Client). Long polling is less resource-intensive for this specific use case and easier to scale

## Data Model (Schema Design)

A relational schema is essential to maintain the relationships between files, versions and blocks

* **User Table**: UserID, Name, Email
* **Object (File) Table**: ObjectID, Name, LatestHistoryVersion
* **Object History Table**: HistoryID, ObjectID, HistoryNumber (Supports versioning)
* **Block Table**: BlockID, HistoryID, Order/Position
* Note: The mapping table connects a specific version of a file to a list of blocks in a 
specific order

## Detailed Workflow

### Upload Flow (Sync Up)

1. **Monitor** detects a file change
2. **Blockify** splits the file into blocks, compresses, and encrypts them
3. **Synchronizer** sends the blocks to the **Block Service** (if they don't exists yet)
4. **Synchronizer** commits the new metadata (new version, list of blocks) to the **Metadata Service**.
5. **Metadata Service** updates the SQL DB and pushes a change event to a Message Queue (e.g Kafka)

### Download Flow (Sync Down)

1. **Notification Service** (listening to Kafka) alerts connected clients via **Long Polling** 
that a file has changed
2. **Client Synchronizer** requests the new metadata from the Metadata Service
3. Client compares the new block list with its local state
4. Client downloads *only* the missing/changed blocks from the **Block Service**
5. **Blockify** reconstructs the file locally.

## Scaling & Trade Off

### Database Sharding
* **Problem**: The metadata DB will grow too large for a single instance
* **Solution**: Horizontal Sharding
* **Sharding Key**: FileID or Hash(FileID). This ensures all metadata for a specific file 
resides in the same shard

### Cold Storage
* **Problem**: Storing 270PB on S3 standard is expensive
* **Solution**: Move rarely accessed data to **Cold Storage** (e.g AWS Glacier)
* **Policy**: Use an Analytics Service to identify files not accessed in X months and migrate 
them

### Deduplication
* **Strategy**: Deduplication happens at block level
* **Impact**: If 100 people upload the exact same "Introduction.pdf", the system only stores
the block once. This significantly reduces storage costs (by ~30-50% in real-world scenarios)

### Consistency Vs Latency (CAP Theorem)
* **Decision**: We choose **Consistency(CP)** for the Metadata DB because file corruption or 
missing versions are unacceptable. We sacrifice some availability/latency during partitions, 
though replication helps mitigate that.

## Deep Dive: Concurrency & Conflict Resolution

**The Problem**
User A and User B both download *Report.txt* (Version 1)
* UserA edits and uploads (Version 2)
* UserB edits and uploads (Version 2)
* If we simply accept UserB's upload, UserA's work is overwritten (Lost Update Problem)

**The Solution**
We don't lock the files while users edit them (that would kill offline mode). Instead: 
1. **Version Check**: When UserB tries to commit changes, they send *original_version: 1*
along with the request
2. **Server Logic**: The Metadata Service checks the DB. It sees the current version as 2 
(from UserA)
3. **Rejection**: The server rejects UserB's commit with a *409 Conflict* error
4. **Resolution Strategies (Client Side)**: 
    * **Strategy A(Dropbox style)**: The client creates a "Conflicted copy" of the file
    (Report (UserB's Conflicted Copy).txt) and uploads that as a new file. Both versions
    exist. The Human user must merge them
    * **Strategy B(Git Style)**: The client downloads the specific changed blocks from
    Version 2, attemtps local merge, and then uploads a new Version 3.

## Security & Permissions (ACLs)
At L4/SDE2s, "Encryption" is not enough. You must define Access Control.

**Access Control List (ACL)**
We need a permissions table in the MetadataDB to handle sharing
* **Table**: User_File_Permissions
* **Columns**: FileID, UserID, AccessType (READ, WRITE, OWNER)
* **Logic**: Before generating a download link or accepting a block, the Metadata Service
queries this table

**Authentication & Authorization**
* **AuthN**: Handled via a separate identity service (OAuth2/OIDC). The client sends a 
JWT (JSON web token) with every request
* **AuthZ**: The Metadata Service validates the JWT signature, extracts the *UserID*, and 
checks the ACL table

**Security In Transit & Rest**
* **TLS (SSL)**: All data in transit (Client <-> Load Balancer)
* **Encryption at Rest**:
    * **Blocks**: Encrypted using envelope encryption (Data Key encrypted by a Master Key in
    AWS KMS)
    * **Metadata**: Database storage encryption

## API Design (Contract Definition)

1. POST /files/upload_session
Initiates an upload
* Request: { file_name: "photo.jpg", file_size: 10MB, checksum: "sha256..." }
* Response: { upload_id: "xyz", presigned_url: "https://s3.aws..." }
**SDE2 Pro Tip**: Use **Presigned URLs**. Do not stream file data through your API servers.
Let the client upload blocks directly to the Cloud Storage (S3) using a temporary, secure 
URL generated by the server. This reduces load on your web servers

2. POST /files/commit
Called after blocks are uploaded to S3 
**Request**
{
  "upload_id": "xyz",
  "block_list": ["block_hash_1", "block_hash_2"],
  "parent_folder_id": "folder_123"
}
**Response**
Response: { file_id: "file_999", version: 1 }

3. GET /files/metadata
**Request**: { file_id: "file_999" }
**Response**: Returns block list, version, owner info

## Failure Handling & Resilience

What happens when things break ? 

**Client-side Failures**
* **Network Cutoff**: If an upload cuts off at 90%, the client has the *upload_id*, 
it can retry only the missing blocks (Resumable Uploads)
* **Exponential Backoffs**: If the server is overloaded (5xx error), the client must 
retry with increasing delays (1s, 2s, 4s, ...) to prevent a "thundering herd"

**Server-side Failures**
* **Atomicity**: Updating the metadata DB and notifying the Notification Service must 
appear atomic
    Pattern: **Transactional Outbox**: Save the metadata AND the notification event to the 
SQL DB in the same transaction. A separate background worker reads the event table and pushes
to Kafka. This ensures we never lose a notification even if the message queue is down

## Advanced Storage Optimizations

### Data Deduplication (Global Vs User)
* **Post-Process Deduplication**: Doing deduplication in real-time (inline) adds latency
* **Architecture Adjustment**:
    1. Client uploads blocks blindly
    2. An asynchronous "**Dedup Worker**" scans the storage bucket
    3. If it finds identical block hashes, it deletes the duplicate and updates the reference 
        to point to the existing block 
    * *Trade-off*: Uses more temporary storage space but ensures fast uploads for the user

### Trash/Lifecycle Management
* **Soft Delete**: When a user deletes a file, simply set a flag *is_deleted=True* in the DB.
Do not remove blocks immediately
* **Garbage Collection**: A cron job runs daily. It finds files marked deleted > 30 days ago.
It removes the metadata and decrements the reference count on the associated blocks. If a 
block's reference count hits 0, the block is deleted from S3.


## References
https://www.youtube.com/watch?v=4Y5EYvQcMqI


