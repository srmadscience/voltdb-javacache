package org.voltdb.jsr107.test;

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

import java.io.Serializable;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

public class MyCacheEntryListener<K, V> implements CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>,
        CacheEntryRemovedListener<K, V>, Serializable {
    private static final long serialVersionUID = 1L;

    int created = 0;
    int updated = 0;
    int deleted = 0;

    @SuppressWarnings("unused")
    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends K, ? extends V> event : events) {
            created++;
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends K, ? extends V> event : events) {
            deleted++;
        }

    }

    @SuppressWarnings("unused")
    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends K, ? extends V> event : events) {
            updated++;
        }

    }

    /**
     * @return the created
     */
    public int getCreated() {
        return created;
    }

    /**
     * @return the updated
     */
    public int getUpdated() {
        return updated;
    }

    /**
     * @return the deleted
     */
    public int getDeleted() {
        return deleted;
    }

    public void resetCounters() {
        created = 0;
        updated = 0;
        deleted = 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MyCacheEntryListener [created=");
        builder.append(created);
        builder.append(", updated=");
        builder.append(updated);
        builder.append(", deleted=");
        builder.append(deleted);
        builder.append("]");
        return builder.toString();
    }

}
