package org.voltdb.jsr107;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2021 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;

import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Runnable class to read Kafka messages and log them.
 *
 */
public class CacheEventConsumer implements Runnable {

    /**
     * Comma delimited list of Kafka hosts. Note we expect the port number with each
     * host name
     */
    String kafkaHostnames;

    String cacheName;

    /**
     * Keep running until told to stop..
     */
    boolean keepGoing = true;

    CacheEntryListenerConfiguration<String, byte[]> celc;

    VoltDBCache cache = null;

    /**
     * Used for formatting messages
     */
    static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Create a runnable instance of a class to poll the Kafka topic
     * console_messages
     * 
     * @param hostnames - hostname1:9092,hostname2:9092 etc
     * @param string
     */
    public CacheEventConsumer(String cacheName, String kafkaHostnames, CacheEntryListenerConfiguration<String, byte[]> celc,
            VoltDBCache cache) {
        super();
        this.cacheName = cacheName;
        this.kafkaHostnames = kafkaHostnames;
        this.celc = celc;
        this.cache = cache;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {

        Factory<CacheEntryListener<? super String, ? super byte[]>> listenerFactory = celc
                .getCacheEntryListenerFactory();
        CacheEntryListener<? super String, ? super byte[]> l = listenerFactory.create();

        Factory<CacheEntryEventFilter<? super String, ? super byte[]>> filterFactory = celc
                .getCacheEntryEventFilterFactory();
        CacheEntryEventFilter<? super String, ? super byte[]> filter = null;

        if (filterFactory != null) {
            filter = filterFactory.create();
        }

        try {

            Properties props = new Properties();
            props.put("bootstrap.servers", kafkaHostnames);
            props.put("group.id", cacheName);
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("auto.commit.interval.ms", "100");
            props.put("auto.offset.reset", "latest");

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList("kv_deltas"));

            while (keepGoing) {

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); 
                for (ConsumerRecord<String, String> record : records) {
                    CacheEventConsumer.msg(record.toString());

                    KVEvent event = KVEvent.createEventFromKafka(cache, record.value().toString());

                    if (event.getCacheName().equals(cacheName)) {

                        if (filter == null || filter.evaluate(event)) {

                            if (event.getEventType() == EventType.CREATED && l instanceof CacheEntryCreatedListener) {
                                ((CacheEntryCreatedListener<? super String, ? super byte[]>) l).onCreated(event);
                            } else if (event.getEventType() == EventType.UPDATED
                                    && l instanceof CacheEntryUpdatedListener) {
                                ((CacheEntryUpdatedListener<? super String, ? super byte[]>) l).onUpdated(event);
                            } else if (event.getEventType() == EventType.REMOVED
                                    && l instanceof CacheEntryUpdatedListener) {
                                ((CacheEntryRemovedListener<? super String, ? super byte[]>) l).onRemoved(event);
                            } else if (event.getEventType() == EventType.EXPIRED
                                    && l instanceof CacheEntryExpiredListener) {
                                ((CacheEntryExpiredListener<? super String, ? super byte[]>) l).onExpired(event);
                            }
                        }
                    }
                }

            }

            consumer.close();

        } catch (Exception e1) {
            CacheEventConsumer.msg(e1);
        }

    }

    /**
     * Stop polling for messages and exit.
     */
    public void stop() {
        keepGoing = false;

    }

    /**
     * Print a formatted message.
     * 
     * @param message
     */
    public static void msg(String message) {

        Date now = new Date();
        String strDate = sdfDate.format(now);
        System.out.println(strDate + ":" + message);

    }

    /**
     * Print a formatted message.
     * 
     * @param e
     */
    public static void msg(Exception e) {

        Date now = new Date();
        String strDate = sdfDate.format(now);
        System.out.println(strDate + ":" + e.getClass().getName() + ":" + e.getMessage());

    }

}
