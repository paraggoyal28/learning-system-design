# Scalable Key Value Cache 

## Use Cases
* User sends a search request resulting in a cache hit
* User sends a search request resulting in a cache miss
* Service has high availability

## Constraints and assumptions
**State assumptions**
* Traffic is not evenly distributed
    * Popular queries should almost always be in the cache
    * Need to determine how to expire/refresh
* Serving from cache requires fast lookups
* Low latency between machines
* Limited memory in cache
    * Need to determine what to keep/remove
    * Need to cache millions of queries
* 10 million users
* 10 billion queries per month -> 4000 queries/sec

**Calculate usage**
* Cache stores ordered list of key: query,value : results
    * query - 50 bytes
    * title - 20 bytes
    * snippet - 200 bytes
    * total - 270 bytes
* 2.7 TB of cache data per month if all 10 million queries are unique and all are stored
    * 270 bytes per search * 10 billion searches per month
    * Assumptions state limited memory, need to determine how to expire contents

**Handy conversion guide**:
* 2.5 million seconds per month
* 1 request per second = 2.5 million requests per month
* 40 requests per second = 100 million requests per month
* 400 requests per second = 1 billion requests per month

## High Level System Design
![architecture for key-value store](image.png)

## Design core components

### Use Case: User sends a request resulting in a cache hit

Popular queries can be served from **Memory cache** such as Redis or Memcached to reduce read 
latency and to avoid overloading the **Reverse Index Service** or **Document Service**. Reading
1 MB sequentially from memory takes about 250 microseconds, while reading from SSD takes 4x and 
from disk takes 80x longer.

Since the cache has limited capacity, we'll use a least recently used (LRU) approach to expire
older entries:

* The **Client** sends a request to the **Web Server**, running as a **reverse proxy**
* The **Web Server** forwards the request to the **Query API** server.
* The **Query API** server does the following:
    * Parses the query
        * Removes markup
        * Breaks up the text into terms
        * Fixes typos
        * Normalizes capitalization
        * Converts the query to use boolean operations
    * Checks the **Memory Cache** for the content matching the query
        * If there's hit in the **Memory Cache**, the **Memory Cache** does the following:
            * Updates the cached entry's position to the front of the LRU list
            * Returns the cached content
        * Else, the **Query API** does the following:
            * Uses the **Reverse Index Service** to find documents matching the query
                * The **Reverse Index Service** ranks the matching results and returns the top one
            * Uses the **Document Service** to return titles and snippets
            * Updates the **Memory Cache** with the contents, placing the entry to the front of 
            LRU list

## Cache implementation

The cache can use a doubly linked list, new items will be added to the head while items to 
expire will be removed from the tail. We'll use a hash table for fast lookups to each linked
list node

**Query API Server** Implementation:

class QueryApi(object):
    def __init__(self, memory_cache, reverse_index_service):
        self.memory_cache = memory_cache
        self.reverse_index_service = reverse_index_service

    def parse_query(self, query):
        '''
        Removes markup, break text into terms, deal with typos, 
        normalizes capitalization, convert to use boolean operations
        '''

    def process_query(self, query):
        query = self.parse_query(query)
        results = self.memory_cache.get(query)
        if results is None:
            results = self.reverse_index_service.process_search(query)
            self.memory_cache.set(results, query)
        return results
    
**Node** implementation:
class Node(object):
    def __init__(self, query, results):
        self.query = query
        self.results = results
    
**LinkedList** implementation
class LinkedList:
    def __init__(self):
        self.head = head
        self.tail = tail
    
    def move_to_front(self, node):
        .....
    
    def append_to_front(self, node):
        ......
    
    def remove_from_last(self, node):
        .....
    
**Cache** implementation:

class Cache(object):

    def __init__(self, MAX_SIZE):
        self.MAX_SIZE = MAX_SIZE
        self.size = 0
        self.lookup = {} # key: query, value: node
        self.linkedlist = LinkedList()
    
    def get(self, query):
        node = self.lookup(query)
        if node is None:
            return None
        self.linked_list.move_to_front(node)
        return node.results
    
    def set(self, results, query):
        node = self.lookup(query)
        if node is not None:
            # Key exists in cache, updates the value
            node.results = results
            self.linked_list.move_to_front(node)
        else:
            # Key does not exist in cache
            if self.size == self.MAX_SIZE:
                # Remove the oldest entry from the linked list and lookup
                self.lookup.pop(self.linked_list.tail.query, None)
                self.linked_list.remove_from_tail()
            else:
                self.size += 1
            
            # Add the new key and value
            new_node = Node(query, results)
            self.linked_list.append_to_front(new_node)
            self.lookup[query] = new_node

**When to update the cache**
The cache should be updated when:
* The page contents changes
* The page is removed or a new page is added
* The page rank changes

The most straightforward way to handle these cases is to simply set a max time that
a cached entry can stay in the cache before it is updated, referred to as time to live (TTL)




