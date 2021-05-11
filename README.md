# voltdb-javacache

voltdb-javacache is a Proof-Of-Concept implementation of a [JSR107](https://github.com/jsr107/jsr107spec) compliant Java Cache.

## Description

This project has a class called [VoltDBCache](src/org/voltdb/jsr107/VoltDBCache.java) which implements [javax.cache.Cache](https://github.com/jsr107/jsr107spec/blob/master/src/main/java/javax/cache/Cache.java). 

This API is used as a basis for multiple imlementations:

* Hazelcast / GridGain
* Oracle Coherence
* Terracotta Ehcache
* Infinispan

VoltDBCache doesn't actually cache anything. Instead it's a cunningly disguised API for VoltDB.

Internally data is stored in a VoltDB Table:

    CREATE TABLE kv 
    (c varchar(30) not null 
    ,k varchar(128) not null 
    ,v varbinary(1048576)
    ,primary key (c, k));

    PARTITION TABLE kv ON COLUMN k;


* 'c' is the name of the cache, as we assume you might want more than one cache and don't want to share a namespace.
* 'k' is the key. We assume it's a string
* 'v' is a long var binary (byte[])

We also have an output stream that is connected to a Kafka Topic:

    CREATE STREAM kv_deltas 
    PARTITION ON COLUMN k 
    (c varchar(30) not null 
    ,k varchar(128)  not null
    ,v varbinary(1048576)
    ,event_type varchar(1));
    
    CREATE TOPIC USING STREAM kv_deltas
     PROPERTIES (consumer.keys='k');

This is used by our implementation of [CacheEventConsumer](src/org/voltdb/jsr107/CacheEventConsumer.java)

## Usage

    org.voltdb.jsr107.VoltDBCache.VoltDBCache(String hostnames, int retryAttempts, String cacheName, String entryProcessorDirName, String entryProcessorPackageName, int kafkaPort)
    
    Parameters:
    hostnames comma delimited list of hostnames that make up the VoltDB cluster
    retryAttempts How many times we try to speak to VoltDB before giving up. Exponential backoff is in use.
    cacheName name of our cache.
    entryProcessorDirName If we are using Invoke this is the physical directory the .class files live in.
    entryProcessorPackageName If we are using Invoke this is the package name our Invokeable classes use
    kafkaPort - kafka port number on VoltDB, usually 9092

For example:

    VoltDBCache mycache = new VoltDBCache("localhost", 10, "MYCACHE",
                "/Users/drolfe/Desktop/EclipseWorkspace/voltdb-javacache/bin", "jsr107.test", 9092);

Once you have a cache you can use all the methods defined in [javax.Cache](https://github.com/jsr107/jsr107spec/blob/master/src/main/java/javax/cache/Cache.java)

## Known Limitations

### Sync operations

The API is fundamentally sync in nature, whereas VoltDB is fundamentally async. We may extend the base Cache classes in future to resovle this. It is interesting to note that several imoplementers of Cache added their own async extensions. 

### HA and group methods

By default the Cache will make as many attempts as it needs to to communicate with VoltDB. In the current implemention this is not guranteed for:

* [getAll](https://github.com/jsr107/jsr107spec/blob/master/src/main/java/javax/cache/Cache.java#L121) 
* [putAll](https://github.com/jsr107/jsr107spec/blob/master/src/main/java/javax/cache/Cache.java#L278)
* [invokeAll](https://github.com/jsr107/jsr107spec/blob/master/src/main/java/javax/cache/Cache.java#L640)

### invoke and invokeAll

These methods assume code that implements [EntryProcessor](https://github.com/jsr107/jsr107spec/blob/master/src/main/java/javax/cache/processor/EntryProcessor.java) has been loaded onto the server. This is not done automatically - you need to call the non-API method 'loadEntryProcessors()' first. 

### iterator will fail if asked to return > 50MB of data

the iterator() method attempts to unload the entire contents of the Cache into a data structure, which is not a great idea if you are suing VoltDB. It will work on small caches but throw a 'Too Much Data Requesrted' CacheException otherwise.

### EntryProcessor

Any class that implements EntryProcessor can be used by Invoke,  but implementors need to remember that it runs inside VoltDB, possibly more than once at the same time, so:

* It must be determistic in nature. No random behaviour, network activity etc.
* It needs to be reasonably fast.
* It needs to exist as a stand alone class file in the directory whose name is passed in to VoltDBCache at creation time
* All EntryProcessors need to be in the package whose name is passed in to VoltDBCache at creation time
* It must use the org.voltdb.autojar.IsNeededByAVoltDBProcedure annotation.
## Next Steps

* We're looking at an async extension

