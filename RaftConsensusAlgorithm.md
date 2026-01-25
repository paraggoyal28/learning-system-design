# Raft Consensus Algorithm 

## Introduction
Raft is a consensus algorithm designed to manage a **Replicated State Machine**. It ensures
that a cluster of nodes agrees on a series of commands (the "Log") so that they all arrive to
the same final state.

From a system design prespective, Raft is a CP (Consistent + Partition Tolerant) system.
It sacrifies availability during a network partition (specifically, the minority partition
goes offline) to guarantee strong consistency (Linearizability).

**Key Use Cases**: etcd (Kubernetes backing store), Consul, CockroachDB, Kafka (KRaft Mode)

## Core Architecture
### Node States
At any given time, a server in a Raft cluster is in one of the three states. The transitions are
triggered by timeouts or RPC messages.

* **Follower**: Passive. Issues no requests but simply responds to leaders and candidates
* **Candidates**: Active. It creates a new "Term" and asks for votes to become the leader.
* **Leader**: Dominant. It handles all client requests and replicates entries to followers.

### Terms (Logical clocks)
Raft divides time into arbitrary lengths called **Terms**. Terms are numbered with consecutive 
integers.

* Acts as a logical clock (Lamport Clock equivalent)
* Allows nodes to detect state information. If a node receives a request with *term < currentTerm*, it rejects it immediately.

### RPC Communication
Raft nodes communicate via two primary Remote Procedure Calls (RPCs)
1. **RequestVotes RPC**: Sent by candidates during elections.
2. **AppendEntries RPC**: Sent by leaders to replicate log entries and as "Heartbeats" 
(empty entries) to maintain authority.

## Phase 1: Leader Election
Raft uses a *Strong Leader* model. The system cannot function (cannot accept writes) without
a leader.

**The Election Loop**
1. **Heartbeats**: The Leader sends periodic heartbeats to all followers.
2. **Election Timeout**: If a follower hears nothing for a randomized duration (e.g 150ms, 300ms), it assumes the leader is dead.
3. **Candidacy**: 
    * Increments *currentTerm*
    * Votes for itself.
    * Sends *RequestVote* RPCs to all other nodes.
4. **Victory**: A candidate wins if it receives votes from a *Quorum* (Majority: N/2 + 1)

**Solving Split Votes**
If multiple followers time out simultaneously, they might split the vote (e.g 5 nodes, 2 vote
for A, 2 vote for B, 1 abstains). Raft solves this using **Randomized Election Timeouts**

* Because timeouts are random, one node will likely time out slightly earlier than the 
others, launch an election, and wins before others wake up.

## Phase 2: Log Replication

This is the "core" consistency engine. The goal is to ensure the **Log** is identical across all nodes.

**The Replication Flow**
1. **Client Request**: Client sends command **SET X = 5** to Leader.
2. **Local Append**: Leader appends command to its own log.
3. **Broadcast**: Leader sends *AppendEntries* to all followers.
4. **Majority Check**: Once the leader gets success responses from a majority, the entry
is committed.
5. **Apply**: Leader executes the command in its State Machine, returns result to client, and 
notifies followers to commit.

**The Log Matching Property**
This is the most critical invariant for data integrity. *AppendEntries* contains the new entry
plus the *index* and *term* of previous entry.
* **Check**: When a Follower receives a new entry, it checks if its log matches the Leader's
previous entry.
* **Failure**: If they don't match, the Follower rejects the new entry.
* **Retry**: The Leader decrements the *nextIndex* for that follower and retries until the 
logs match. This effectively forces the follower's log to match the Leader's

## Phase 3: Handling Failures

### The "Split Brain" Problem (Network Partitions)
As discussed, if a cluster splits into (A, B) and (C, D, E).

* **Minority** (A, B): A remains Leader but cannot commit (cannot reach majority). Writes
sent here times out.
* **Majority** (C, D, E): Elects C as new leader. Can commit writes.
* **Healing**: When the partition heals, A sees C has a higher term. A steps down and 
**truncates** its uncommitted logs to match C. So the writes happening after the partition
and before C is assigned as the new leader are all lost, and that is fine because the client 
is not sent a success response for those requests.

### Safety: Election Restriction
Raft prevents a node from becoming a leader if it contains stale data

* **Rule**: A Candidate includes its last log index and term in the *RequestVote* RPC.
* **Voter Logic**: A node denies the vote if its own log is "more up to date" than the 
candidate's
* **Result**: this guarantees that any elected leader holds **all committed entries**

## Advanced Concepts

### Log Compaction (Snapshotting)
In a real system, the log cannot grow forever (disk space is finite)
* **Snapshot**: Periodically, the state machine saves its current state (eg. "X-100") to a
snapshot file
* **Truncation**: All log entries leading up to that state are discarded.
* **Lagging Followers**: If a follower is too far behind (its needed log entries were
discarded), the Leader sends a *InstallSnapshot* RPC to send the entire state chunk instead of 
individual log entries.

### Client interaction
* **Finding Leader**: Clients can connect to any node. If it's not the leader, the node 
redirects the client to the known leader.
* **Idempotency**: If a Leader crashes after committing but before responding, the client
will retry. The system must implement unique serial numbers (UIDs) for commands so the new
Leader recognizes the retry and doesn't execute the command twice.


