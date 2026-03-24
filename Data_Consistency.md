# Linearizable Consistency

Show all changes in the database before current read request
Means all changes which have happened in the database before the read operation
will be reflected in the read query

Suppose
x = 10
update x to 13
update x to 17
read x - 17 is returned
update x to 1
read x - 1 

To achieve this, we use a single threaded server. So every read and write request
will always be ordered. Using the above example, the first read x will be executed after
updating the value to 17. This is useful when systems need perfect consistency.

Linearizable consistency to maintain it using RAFT algorithm

# Eventual Consistency

Requests reach server in parallel.
Requests running concurrently, maybe on different threads.
Requests are running concurrently in the database itself.

We might get the responses for the above requests out of order.
Lets say we ask for Write(1, 31) and then Read(1) but may be Read(1) happens first
and then Write(1, 31), then previous value for the id 1 is returned lets say 21.

For some applications, the above eventual consistency is not fine.
for eg an e-commerce applications where if for eg we add an item to the cart and then
removed it, and it is not reflected immediately then that would be bad user experience.

Otherwise for eg., if a user sends a mail, it goes in our outbox. After sometime, it 
will go in the sent box. 
May be refreshing a couple of times will not show the mail in outbox, but we know 
after some time it will be there in the outbox. So in this case eventual consistency is fine.


# Casual Consistency

At this consistency level, if a previous operation is related to the current operation,
then the previous operation must be executed before the current operation.
Casual consistency is stronger and slower than eventual consistency because 
operations for the same key are processed sequentially.
It is looser and faster than the serializable consistency level because it does not
wait for all previous operations to complete.
Causal consistency fails when performing aggregation operations.



