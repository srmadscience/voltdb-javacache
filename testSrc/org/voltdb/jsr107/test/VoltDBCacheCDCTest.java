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

import static org.junit.jupiter.api.Assertions.*;

import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.voltdb.jsr107.VoltDBCache;

class VoltDBCacheCDCTest {

    private static final String FRED_TEST_CDC = "FREDtestCDC";

    VoltDBCache c = null;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {

        VoltDBCache.msg("Start");

    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {

    }

    @BeforeEach
    void setUp() throws Exception {

        c = new VoltDBCache("localhost", 10, "FRED", "", "", 9092);
        c.removeAll();

    }

    @AfterEach
    void tearDown() throws Exception {

        c.clear();
        c.close();
    }

    @Test
    @Order(1)
    void testCDC() {

        Factory<MyCacheEntryListener<String, byte[]>> theListenerFactory = new MyCacheEntryListenerFactory();
        Factory<MyCacheEntryEventFilter<String, byte[]>> theEventFactory = new MyCacheEntryFilterFactory(FRED_TEST_CDC);

        MutableCacheEntryListenerConfiguration<String, byte[]> fred = new MutableCacheEntryListenerConfiguration<String, byte[]>(
                theListenerFactory, theEventFactory, false, true);

        c.registerCacheEntryListener(fred);

        c.deregisterCacheEntryListener(fred);

        c.setEvents(true);

        c.registerCacheEntryListener(fred);

        MyCacheEntryListener<String, byte[]> l = ((MyCacheEntryListenerFactory) theListenerFactory).getListener();

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            fail(e);
        }

        l.resetCounters();

        if (l.getCreated() != 0 || l.getUpdated() != 0 || l.getDeleted() != 0) {
            fail("early records");
        }

        final int insertCount = 1000;
        final int updateCount = 999;
        final int deleteCount = 998;

        for (int i = 0; i < insertCount; i++) {
            c.put(FRED_TEST_CDC + i, "FRED".getBytes());
        }

        for (int i = 0; i < updateCount; i++) {
            c.put(FRED_TEST_CDC + i, "FREDUPDATE".getBytes());
        }

        for (int i = 0; i < deleteCount; i++) {
            c.remove(FRED_TEST_CDC + i);
        }

        long timeoutMS = System.currentTimeMillis() + 20000;

        while (System.currentTimeMillis() < timeoutMS) {

            if (l.getCreated() == insertCount && l.getUpdated() == updateCount && l.getDeleted() == deleteCount) {
                break;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                fail(e);
            }

        }

        System.out.println(l);

        if (System.currentTimeMillis() >= timeoutMS) {
            fail("timeout");
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            fail(e);
        }

        if (l.getCreated() != insertCount || l.getUpdated() != updateCount || l.getDeleted() != deleteCount) {
            fail("late records");
        }

    }

}
