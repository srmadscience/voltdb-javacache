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

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.voltdb.autojar.IsNeededByAVoltDBProcedure;

import jsr107.AbstractEventTrackingProcedure;

@IsNeededByAVoltDBProcedure
public class KVEvent extends CacheEntryEvent<String, byte[]> implements Iterable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    String cacheName;
    String key;
    byte[] value;

    public KVEvent(Cache<String, byte[]> source, EventType eventType, String cacheName, String key, byte[] value) {
        super(source, eventType);
        this.key = key;
        this.cacheName = cacheName;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        return null;
    }

    @Override
    public byte[] getOldValue() {

        return null;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    /**
     * @return the cacheName
     */
    public String getCacheName() {
        return cacheName;
    }

    @Override
    public boolean isOldValueAvailable() {
        return false;
    }

    @Override
    public Iterator<KVEvent> iterator() {

        List<KVEvent> events = new ArrayList<KVEvent>();
        events.add(this);
        return events.iterator();
    }

    public static KVEvent createEventFromKafka(Cache<String, byte[]> source, String record) throws DecoderException {

        EventType eventType = null;
        String cacheName;
        String key;
        byte[] value;

        String[] recordParts = record.split(",");

        cacheName = recordParts[0];
        key = recordParts[1];
        value = Hex.decodeHex(recordParts[2]);

        if (recordParts[3].equals(AbstractEventTrackingProcedure.CREATED)) {
            eventType = EventType.CREATED;
        } else if (recordParts[3].equals(AbstractEventTrackingProcedure.REMOVED)) {
            eventType = EventType.REMOVED;
        } else if (recordParts[3].equals(AbstractEventTrackingProcedure.UPDATED)) {
            eventType = EventType.UPDATED;
        } else if (recordParts[3].equals(AbstractEventTrackingProcedure.EXPIRED)) {
            eventType = EventType.EXPIRED;
        }

        return new KVEvent(source, eventType, cacheName, key, value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("KVEvent [cacheName=");
        builder.append(cacheName);
        builder.append(", key=");
        builder.append(key);
        builder.append(", value=");
        builder.append(Arrays.toString(value));
        builder.append("]");
        return builder.toString();
    }
    
   

}
