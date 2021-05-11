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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.voltdb.jsr107.VoltDBCache;
import org.voltdb.jsr107.VoltDBEntryProcessorResult;

import jsr107.AbstractEventTrackingProcedure;
import jsr107.test.AppenderEntryProcessor;
import jsr107.test.DeleteIfFoundEntryProcessor;
import jsr107.test.SwapperEntryProcessor;

class VoltDBCacheTest {

   
    private static final String FIRST_CACHE_NAME = "Test";
    private static final String SECOND_CACHE_NAME = "Test2";
    private static final byte[] OTHER_BYTES = "Other".getBytes();
    private static final String BAR = "Bar";
    private static final String FOO = "Foo";

    private static final byte[] FOO_BYTES = FOO.getBytes();
    private static final byte[] BAR_BYTES = BAR.getBytes();

    VoltDBCache c = null;
    VoltDBCache c2 = null;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {

        VoltDBCache.msg("Start");

    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {

    }

    @BeforeEach
    void setUp() throws Exception {

        c = new VoltDBCache("localhost", 10, FIRST_CACHE_NAME,
                "/Users/drolfe/Desktop/EclipseWorkspace/voltdb-javacache/bin", "jsr107.test", 9092);
        c.removeAll();

        c2 = new VoltDBCache("localhost", 10, SECOND_CACHE_NAME,
                "/Users/drolfe/Desktop/EclipseWorkspace/voltdb-javacache/bin", "jsr107.test", 9092);
        c2.removeAll();

    }

    @AfterEach
    void tearDown() throws Exception {

        c.clear();
        c.close();
        c2.clear();
        c2.close();
    }

    @Test
    void testGetAndPut() {
        try {

            byte[] fooBytes = FOO_BYTES;

            byte[] oldPayload = c.getAndPut(FOO, fooBytes);

            if (oldPayload != null) {
                fail("Not null not expected");
            }

            byte[] oldPayload2 = c.getAndPut(FOO, fooBytes);

            if (!Arrays.equals(oldPayload2, fooBytes)) {
                fail("Not same");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testGetCacheManager() {
        try {

            CacheManager cm = c.getCacheManager();

            if (cm != null) {
                fail("Not null not expected");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testPutIfAbsent() {
        try {

            byte[] otherPayload = BAR_BYTES;

            byte[] payload = c.get(FOO);

            if (payload != null) {
                fail("Non null not expected");
            }

            payload = FOO_BYTES;

            if (!c.putIfAbsent(FOO, payload)) {
                fail("putIfAbsent - should have worked");
            }

            if (!c.containsKey(FOO)) {
                fail("putIfAbsent  - not found after put");
            }

            if (c.putIfAbsent(FOO, otherPayload)) {
                fail("putIfAbsent - should not have worked");
            }

            byte[] actualPayload = c.get(FOO);

            if (!Arrays.equals(actualPayload, payload)) {
                fail("putIfAbsent - payload changed");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testPut() {
        try {

            byte[] payload = c.get(FOO);

            if (payload != null) {
                fail("Non null not expected");
            }

            payload = FOO_BYTES;

            c.put(FOO, payload);

            if (!c.containsKey(FOO)) {
                fail("put  - not found after put");
            }

            if (c2.containsKey(FOO)) {
                fail(FOO + " in wrong cache");
            }

            // See if throws exception when already there...shouldn't..
            c.put(FOO, payload);

            if (!c.containsKey(FOO)) {
                fail("put  - not found after put");
            }

            byte[] actualPayload = c.get(FOO);

            if (!Arrays.equals(actualPayload, payload)) {
                fail("put - payload changed");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testReplace() {
        try {

            if (c.replace(FOO, BAR_BYTES)) {
                fail("Relace should not work here");
            }

            createFooEntry();

            if (!c.replace(FOO, BAR_BYTES)) {
                fail("Replace should  work here");
            }

            byte[] actualPayload = c.get(FOO);

            if (!Arrays.equals(actualPayload, BAR_BYTES)) {
                fail("Replace didn't");
            }

            byte[] actualPayload2 = c2.get(FOO);

            if (actualPayload2 != null) {
                fail(FOO + " in wrong cache");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testGetAndReplace() {
        try {

            byte[] existing = c.getAndReplace(FOO, BAR_BYTES);

            if (existing != null) {
                fail("get found non-exstent data");
            }

            createFooEntry();

            existing = c.getAndReplace(FOO, BAR_BYTES);

            if (existing == null) {
                fail("getAndReplace didnt find  data");
            }

            if (!Arrays.equals(existing, FOO_BYTES)) {
                fail("Not FOO_BYTES as expected");
            }

            byte[] actualPayload = c.get(FOO);

            if (!Arrays.equals(actualPayload, BAR_BYTES)) {
                fail("getAndReplace didn't");
            }

            actualPayload = c2.get(FOO);
            if (actualPayload != null) {
                fail(FOO + " in wrong cache");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testReplaceKV() {
        try {

            if (c.replace(FOO, FOO_BYTES, BAR_BYTES)) {
                fail("Replace should not work here");
            }

            createFooEntry();

            if (c.replace(FOO, OTHER_BYTES, BAR_BYTES)) {
                fail("Replace should not work here");
            }

            if (!c.replace(FOO, FOO_BYTES, BAR_BYTES)) {
                fail("Replace should  work here");
            }

            byte[] actualPayload = c.get(FOO);

            if (!Arrays.equals(actualPayload, BAR_BYTES)) {
                fail("Replace didn't");
            }

            actualPayload = c2.get(FOO);
            if (actualPayload != null) {
                fail(FOO + " in wrong cache");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testGetAll() {
        try {

            createFooEntry();
            createBarEntry();

            Set<String> theSet = null;

            try {
                c.getAll(null);
                fail("NPE");
            } catch (NullPointerException e) {
                // Expected
            }

            theSet = new HashSet<String>();

            Map<String, byte[]> aMap = c.getAll(theSet);
            if (aMap.size() > 0) {
                fail("Bad Map");

            }

            theSet.add(FOO);

            aMap = c.getAll(theSet);
            if (aMap.size() != 1) {
                fail("Bad Map");

            }

            aMap = c2.getAll(theSet);
            if (aMap.size() != 0) {
                fail(FOO + " in wrong cache");

            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testRemove() {
        try {

            createFooEntry();

            if (c.remove(BAR)) {
                fail("Bar should not exist");
            }

            if (!c.remove(FOO)) {
                fail("Foo not deleted");
            }

            if (c.remove(FOO)) {
                fail("Foo not deleted first time...");
            }

            createFooEntry();
            if (c2.remove(FOO)) {
                fail(FOO + " in wrong cache");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testRemoveKV() {
        try {

            createFooEntry();

            if (c.remove(FOO, BAR_BYTES)) {
                fail("Foo deleted");
            }

            if (c.remove(BAR, BAR_BYTES)) {
                fail("Bar deleted");
            }

            if (!c.remove(FOO, FOO_BYTES)) {
                fail("Foo not deleted");
            }

            createFooEntry();
            if (c2.remove(FOO, FOO_BYTES)) {
                fail(FOO + " in wrong cache");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testGetRemove() {
        try {

            createFooEntry();

            byte[] payload = c.getAndRemove(FOO);

            if (c.containsKey(FOO)) {
                fail("Foo still exists");
            }

            if (!Arrays.equals(payload, FOO_BYTES)) {
                fail("Foo data bad");
            }

            createFooEntry();

            payload = c2.getAndRemove(FOO);

            if (payload != null || c2.containsKey(FOO)) {
                fail(FOO + " in wrong cache");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    private void createFooEntry() {

        c.put(FOO, FOO_BYTES);

        byte[] payload = c.get(FOO);

        if (payload == null) {
            fail("Null not expected");
        }

        payload = c2.get(FOO);

        if (payload != null) {
            fail(FOO + " in wrong cache");
        }
    }

    private void createBarEntry() {

        c.put(BAR, BAR_BYTES);

        byte[] payload = c.get(BAR);

        if (payload == null) {
            fail("Null not expected");
        }
    }

    @Test
    void testGetAndPutCycle() {

        try {

            byte[] fooBytes = FOO_BYTES;

            c.put(FOO, fooBytes);

            byte[] payload = c.get(FOO);

            if (payload == null) {
                fail("Null not expected");
            }

            if (!Arrays.equals(payload, fooBytes)) {
                fail("Not same");
            }

            if (!c.containsKey(FOO)) {
                fail("ContainsKey");
            }

            if (c2.containsKey(FOO)) {
                fail(FOO + " in wrong cache");
            }

            if (c.containsKey(BAR)) {
                fail("ContainsKey");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    @Order(2)
    void testPutAll() {

        try {

            try {
                c.putAll(null);
                fail("NPE check");
            } catch (NullPointerException e) {
                // expected
            }

            Map<String, byte[]> aMap = new HashMap<>();

            try {
                c.putAll(aMap);

            } catch (Exception e) {
                fail("Empty Map");
            }

            aMap.put(FOO, FOO_BYTES);
            c.putAll(aMap);

            aMap.put(BAR, BAR_BYTES);
            c.putAll(aMap);

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testPutAllBig() {

        try {
            Map<String, byte[]> aMap = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                aMap.put(FOO + i, FOO_BYTES);
            }

            c.putAll(aMap);

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testRemoveAllWithSet() {

        try {

            try {
                c.removeAll(null);
                fail("NPE check");
            } catch (NullPointerException e) {
                // expected
            }

            Map<String, byte[]> aMap = new HashMap<>();
            aMap.put(FOO, FOO_BYTES);
            aMap.put(BAR, BAR_BYTES);

            try {
                c.putAll(aMap);

            } catch (Exception e) {
                fail("Empty Map");
            }

            Set<String> keySet = new HashSet<String>();

            try {
                c.removeAll(keySet);
            } catch (NullPointerException e) {
                fail("empty check");
            }

            keySet.add(FOO);
            keySet.add(BAR);

            try {
                c.removeAll(keySet);
            } catch (NullPointerException e) {
                fail("empty check");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testRemoveAllWithoutSet() {

        try {

            Map<String, byte[]> aMap = new HashMap<>();
            aMap.put(FOO, FOO_BYTES);
            aMap.put(BAR, BAR_BYTES);

            try {
                c.putAll(aMap);

            } catch (Exception e) {
                fail("Empty Map");
            }

            c2.put(FOO, FOO_BYTES);
            c.removeAll();

            if (!c2.containsKey(FOO)) {
                fail(FOO + " found in wrong cache");
            }

            if (c.containsKey(FOO)) {
                fail("remove failed");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testNull() {

        try {
            try {

                c.get(null);
                fail("null not trapped");

            } catch (NullPointerException e) {
                VoltDBCache.msg("NPE Caught as planned");
            }
        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testInvokeSwap() {

        try {

            c.loadEntryProcessors();

            String oldString = "APPLE PEAR CARROT";
            String newString = oldString.replace("PEAR", "ORANGE");

            c.put("FRED", oldString.getBytes());
            c2.put("FRED", oldString.getBytes());

            SwapperEntryProcessor theSwapper = new SwapperEntryProcessor();

            c.invoke("FRED", theSwapper, "PEAR", "ORANGE");

            String updatedValue = new String(c.get("FRED"));
            String notUpdatedValue = new String(c2.get("FRED"));

            if (!updatedValue.equals(newString)) {
                fail("invoke failed");
            }

            if (!notUpdatedValue.equals(oldString)) {
                fail(FOO + " in wrong cache");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testInvokeAppend() {

        try {

            c.loadEntryProcessors();

            String oldString = "APPLE PEAR CARROT";
            String newString = oldString + "ORANGE";

            c.put("FRED", "APPLE PEAR CARROT".getBytes());

            AppenderEntryProcessor theSwapper = new AppenderEntryProcessor();

            c.invoke("FRED", theSwapper, "ORANGE");

            String updatedValue = new String(c.get("FRED"));

            if (!updatedValue.equals(newString)) {
                fail("invoke failed");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testInvokeRemove() {

        try {

            c.loadEntryProcessors();

            c.put("FRED", "APPLE PEAR CARROT".getBytes());
            c.put("FRED2", "APPLE BEAR CARROT".getBytes());

            DeleteIfFoundEntryProcessor theSwapper = new DeleteIfFoundEntryProcessor();

            c.invoke("FRED", theSwapper, "PEAR");

            byte[] updatedValue = c.get("FRED");

            if (updatedValue != null) {
                fail("invoke failed");
            }

            c.invoke("FRED2", theSwapper, "PEAR");

            updatedValue = c.get("FRED2");

            if (updatedValue == null) {
                fail("invoke failed");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    void testInvokeAll() {

        try {

            c.loadEntryProcessors();

            String oldString = "APPLE PEAR CARROT";
            String newString = oldString.replace("PEAR", "ORANGE");

            Map<String, byte[]> aMap = new HashMap<>();

            try {
                c.putAll(aMap);

            } catch (Exception e) {
                fail("Empty Map");
            }

            for (int i = 0; i < 100; i++) {
                aMap.put(FOO + i, oldString.getBytes());
                aMap.put(BAR + i, oldString.getBytes());
            }

            c.putAll(aMap);

            Set<String> keySet = new HashSet<String>();

            keySet.add(FOO + "20");
            keySet.add(FOO + "30");

            UnknownToDBEntryProcessor badProcessor = new UnknownToDBEntryProcessor();

            Object badResponse = c.invokeAll(keySet, badProcessor, "PEAR", "ORANGE");
            HashMap<String, VoltDBEntryProcessorResult> badResponse2 = (HashMap<String, VoltDBEntryProcessorResult>) badResponse;

            VoltDBEntryProcessorResult foo20 = badResponse2.get(FOO + "20");

            if (foo20.get().getAppStatus() != AbstractEventTrackingProcedure.BAD_CLASSNAME) {
                fail("bad classname");
            }

            SwapperEntryProcessor theSwapper = new SwapperEntryProcessor();

            Object goodResponse = c.invokeAll(keySet, theSwapper, "PEAR", "ORANGE");
            HashMap<String, VoltDBEntryProcessorResult> goodResponse2 = (HashMap<String, VoltDBEntryProcessorResult>) goodResponse;
            if (foo20.get().getAppStatus() != AbstractEventTrackingProcedure.BAD_CLASSNAME) {
                fail("bad classname");
            }

            foo20 = goodResponse2.get(FOO + "20");
            if (foo20.get().getAppStatus() != AbstractEventTrackingProcedure.OK) {
                fail("call failed");
            }

            String updatedValue = new String(c.get(FOO + "20"));

            if (!updatedValue.equals(newString)) {
                fail("invoke failed");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testGetName() {
        String foo = c.getName();

        if (foo == null) {
            fail("null seen");
        }

    }

    @Test
    void testSetEvents() {
        c.setEvents(false);
        c2.setEvents(true);

        boolean cEvents = c.getEvents();
        boolean cEvents2 = c2.getEvents();

        if (cEvents) {
            fail("setEvents");
        }

        if (!cEvents2) {
            fail("setEvents");
        }

        c.setEvents(true);
        c2.setEvents(false);

        cEvents = c.getEvents();

        if (!cEvents) {
            fail("setEvents");
        }

        cEvents = c2.getEvents();

        if (cEvents) {
            fail("setEvents");
        }

    }

    @Test
    void testGetEvents() {
        boolean foo = c.getEvents();

        if (!foo) {
            fail("getEvents");
        }

    }

    @Test
    void testIterator() {

        c.setEvents(false);
        
        try {
            createFooEntry();
            createBarEntry();

            int entryCount = 0;

            while (c.iterator().hasNext()) {
                entryCount++;
            }

            if (entryCount != 2) {
                fail("iteratorCount");
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testIteratorFail() {

        c.setEvents(false);
        Random r = new Random();

        byte[] payload = new byte[10240];

        r.nextBytes(payload);

        long rowCount = 0;

        try {
            Map<String, byte[]> aMap = new HashMap<>();
            for (int i = 0; i < 8000; i++) {
                aMap.put(FOO + rowCount++, payload);
            }

            c.putAll(aMap);

            // This will fail.
            Iterator it = c.iterator();

            fail("failed to fail when expected...");

        } catch (CacheException e) {
            if (!e.getMessage().startsWith(VoltDBCache.TOO_MUCH_DATA_REQUESTED)) {
                fail(e);
            }

        } catch (Exception e) {
            fail(e);
        }

    }

    @Test
    void testMsg() {
        VoltDBCache.msg("Hello World");

    }

    @Test
    void testClosed() {

        try {
            try {

                c.close();
                c.get(FOO);
                fail("closed not trapped");

            } catch (CacheException e) {
                VoltDBCache.msg("CacheException Caught as planned");
            }
        } catch (Exception e) {
            fail(e);
        }

    }

}
