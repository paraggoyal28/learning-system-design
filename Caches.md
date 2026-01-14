# Caches

## In-Memory Vs Centralized Cache

### In-Memory Cache

#### Pros:
* Extremely fast
* Simple to implement
* Great for scenarios where the need is extremely low latency

#### Cons:
* Multiple servers can have duplicate data
* Restarting the server requires re-populating the cache
* Difficult to maintain data consistency across all the caches
(For example, if three servers are mainting count of a particular T-shirt, then decrementing one
won't reflect in other two servers immediately)

### Centralized Cache

#### Pros:
* Data duplication is much lesser
* Total amount of memory used is less
* Scale up the cache server and application server independently
* Can be a Redis instance or Memcached instance

### How is data distributed amongst cache servers
Have multiple cache servers to serve the request
Geographically distributed servers (e.g one in India, one in US)

### What kind of data should be stored in the cache servers
* Option 1: Store what is requested by the server
Duplication can happen
Multiple caches (in different geographical regions) can have same data

* Option 2: Strategically place data to avoid duplication
Indian cache has only indian specific data
US cache has only US specific data
This strategy is called Sharding. The data is split across multiple nodes
based on a shard key. In this case, the shard key is the country code.

Shard key can be based on UserId. 
Range Based sharding on UserId
0-100: Shard 1
101-200: Shard 2

Within each of these splits say Shard 1, the cache node can be scaled
More cache nodes can be added to handle the requests for 0-100 user ids.
Now when a request comes for user ids 0-100, internally somehow system needs to
decide which cache node to be send the request to.
Internally from 0-100 we might have split the data shards from 0-30, 30-60,60-100
Or we could have use hash-based splitting, basically the t-shirt-id is mod 3 and 
then a shard sub node is selected

Since the hash of the t-shirt-id is always same, the same cache node is selected 
always. 
Hash based splitting does
* Load balancing, and 
* No data duplication

### Issue comes: If one of the cache node crashes**
For example, if the cache node 1 fails, all t shirt requests whose id%3 gave 1 
will fail.
Solution: Auto-recover the system. 
Idea is basically change the mod to the number of current servers
for eg. if initially there were 3 and then one failed then previously we did mod 3
now we have to do mod 2.  Same for adding more servers
Problem with the above approach is that when we increment or decrement the servers
we need to move data from the failed instances to the new instances to make 
sure no loss of data occurs.
Similarly for adding new servers some of data have to be sent from the old instances
to the new instances to keep the request load balanced.

**Problem**: Cache warmup or readiness takes a lot of time for the above process of
adjusting the data
**Solution**: Consistent Hashing
Reduce the amount of data migration or data loading when your system scales up or 
scales down.
Number of migrations without consistent hashing is (11/12)th keys in each server
Consistent Hashing works like 
All the requests from 0..2^64 and plotted on a circular ring.
Each cache server has a id, we calculate the hash(id) and then plot those ids on the 
same ring, making sure that the distribution is uniformly random.
Now, whenever a request comes into our system, find the hash of it, and then find the 
closest server (clockwise or anticlockwise on the ring).
Basically it is going to take a server which is smallest greater than the request id hash
Can be easily found with a hash set
If we have to add one more server, we just take the random hash of its id and assign it to
the ring.

### Why does consistent hashing works ? 

* Chance of a server being picked is (1/num_of_servers)
* Adding and removing a server is much easier

#### Disadvantages of consistent hashing

* If we have few servers, chances of skewness are quite high

### Improvement to the algorithm to avoid skewness
If three servers are placed very close to one another, then all requests between 
third and first server are send to either first or third causing that server to 
be overloaded

### Solution to the above problem
**Virtual Points** 
Basically server1 can be seen having multiple virtual server ids 
Each of these virtual ids is mapped on the ring.

### Advantages
* Reduces cache warmup time
* Increased cache readiness
* Used in multiple systems like Amazon DynamoDB, which uses this to reduce the amount of 
data migration between its database servers. Redis also has consistent hashing as one of 
load balancing algorithm it offers.

Google also uses a variation of consistent hashing 
called backend subsetting

## Problems with Caches
* Inconsistent data between DB and Caches
Solution: Update Cache infrequently
For eg. for top 10 t-shirts If we have stored that order information
and then another user visits and the order changes then the cache contains stale
data but that change in the order happens to a very few items within a small period 
of time. 
So it is better to return a slightly stale data instead of querying the database again and 
again. 
TTL can be set for 1 hour or 6 hours

* Objects in cache are very heavy
Solution: Store only the necessary data
Example for each user instead of storing entire t-shirt jsons, store just the t-shirt ids
That prevents the duplication of json for different users
When we have got the ids for a user, we can fetch the entire jsons by another query

* Updates to DB, read from cache (to avoid stale data)
Caches can also be consistent 
The way to keep a cache updated is write policy
Also known as update policy
**Write Policies**
1. Write-Through: All updates to the cache are also updated to the DB
2. Write-Behind: All updates go to the cache initially. And the DB is updated later once 
the update entry in the cache is to be evicted
Bit risky: If  Redis crashes then all those updates are lost.
If its page view cache, then it is ok to use write-behind, if for some time the data is lost
But if its payments cache, then we may need to use write-through

For cache storing User's Recently Viewed T-shirts data, 
the cache might be overflowed with the data size, and we might need to do some
optimizations to reduce that.
One optimization is to only store the data for active users
Two ways of identifying an active user:
1. Users who keep coming to the platform: Users who login the most (Number of logins)
2. Users who are most recently active users: Time to login 

Need to sort the cache entries in one of the above ways
Take top 1000 or 1% entries

What if a new user comes to the cache ? 
What entry needs to be removed ? 

**Policy for eviction**: Least Recently used for recently active users
**Policy for eviction**: Least Frequently used for most number of logins by user

## References

* https://interviewready.io/blog/netflix-load-balancer-connection-churn-subsetting














