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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

import org.voltdb.VoltTable;
import org.voltdb.autojar.AutoJar;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import jsr107.VoltParameterWrangler;

public class VoltDBCache implements Cache<String, byte[]> {

    String hostnames;
    int retryAttempts;
    int retryPower = 2;
    boolean events = false;
    Thread eventConsumerRunner;
    String entryProcessorPackageName;
    String entryProcessorDirName;
    int kafkaPort;
    public static final String TOO_MUCH_DATA_REQUESTED = "Too much data requested";
    /**
     * Used for formatting messages
     */
    static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    Client c = null;

    String cacheName;

    CacheEntryListenerConfiguration<String, byte[]> celc;

    CacheEventConsumer eventConsumer = null;

    /**
     * 
     * @param hostnames  comma delimited list of hostnames that make up the VoltDB cluster
     * @param retryAttempts How many times we try to speak to VoltDB before giving up. Exponential backoff is in use.
     * @param cacheNamea name of our cache. 
     * @param entryProcessorDirName If we are using Invoke this is the physical directory the .class files live in.
     * @param entryProcessorPackageName If we are using Invoke this is the package name our Invokeable classes use
     * @param kafkaPort - kafka port number on VoltDB, usually 9092
     */
    public VoltDBCache(String hostnames, int retryAttempts, String cacheName, String entryProcessorDirName,
            String entryProcessorPackageName, int kafkaPort) {
        super();
        this.hostnames = hostnames;
        this.retryAttempts = retryAttempts;
        this.cacheName = cacheName;
        this.entryProcessorPackageName = entryProcessorPackageName;
        this.entryProcessorDirName = entryProcessorDirName;
        this.kafkaPort = kafkaPort;

        try {
            getClient();
            getEventsFromDB();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void loadEntryProcessors() {
        try {

            if (entryProcessorDirName == null || entryProcessorDirName.length() == 0) {
                throw new NullPointerException("entryProcessorDirName");
            }

            if (entryProcessorPackageName == null || entryProcessorPackageName.length() == 0) {
                throw new NullPointerException("entryProcessorPackageName");
            }

            AutoJar.load(entryProcessorDirName, entryProcessorPackageName, c, null);

        } catch (Exception e) {
            throw new CacheException("loadEntryProcessors:" + e.getMessage());
        }
    }

    private void getEventsFromDB() {
        long eventsFlag;
        try {
            eventsFlag = (long) callVoltDBProc("GetParam", cacheName, "ENABLE_EVENTS");
            if (eventsFlag == 1) {
                events = true;
            } else {
                events = false;
            }
        } catch (NullPointerException e) {
            events = false;
        }

    }

    @Override
    public void clear() {
        // NOOP - we don;t actually cache anything...
    }

    @Override
    public void close() {

        if (c != null) {
            try {
                c.drain();
                c.close();
            } catch (Exception e) {
            }
            c = null;
        }

    }

    @Override
    public boolean containsKey(String arg0) {

        checkNotClosed();

        checkNotNull(arg0);

        Object key = callVoltDBProc("ContainsKey", arg0, cacheName);

        if (key != null) {
            return true;
        }

        return false;
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<String, byte[]> arg0) {

        stopCacheEntryListenerThread();
        celc = null;

    }

    @Override
    public byte[] get(String arg0) {

        checkNotClosed();

        checkNotNull(arg0);

        return (byte[]) callVoltDBProc("Get", arg0, cacheName);
    }

    @Override
    public Map<String, byte[]> getAll(Set<? extends String> arg0) {

        Map<String, byte[]> results = new HashMap<String, byte[]>();

        checkNotClosed();

        checkNotNull(arg0);

        CountDownLatch latch = new CountDownLatch(arg0.size());
        BulkGetCallback bgcc = new BulkGetCallback(latch, results);

        Iterator<?> it = arg0.iterator();
        while (it.hasNext()) {

            String key = (String) it.next();

            try {
                c.callProcedure(bgcc, "GetKV", key, cacheName);
            } catch (IOException e) {
                throw new CacheException("IOException:" + e.getMessage());
            }

        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new CacheException("InterruptedException:" + e.getMessage());
        }

        return results;
    }

    @Override
    public byte[] getAndPut(String arg0, byte[] arg1) {
        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        return (byte[]) callVoltDBProc("GetAndPut", arg0, cacheName, arg1);
    }

    @Override
    public byte[] getAndRemove(String arg0) {

        checkNotClosed();

        checkNotNull(arg0);

        return (byte[]) callVoltDBProc("GetAndRemove", arg0, cacheName);
    }

    @Override
    public byte[] getAndReplace(String arg0, byte[] arg1) {

        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        return (byte[]) callVoltDBProc("GetAndReplace", arg0, cacheName, arg1);
    }

    @Override
    public CacheManager getCacheManager() {

        // Cache is not managed...
        return null;
    }

    @Override
    public <C extends Configuration<String, byte[]>> C getConfiguration(Class<C> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(String arg0, EntryProcessor<String, byte[], T> arg1, Object... arg2)
            throws EntryProcessorException {

        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        checkNotNull(arg2);

        return (T) callVoltDBProc("Invoke", arg0, cacheName, arg1.getClass().getName(),
                VoltParameterWrangler.convertToVoltTable(arg2));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Map<String, EntryProcessorResult<T>> invokeAll(Set<? extends String> arg0,
            EntryProcessor<String, byte[], T> arg1, Object... arg2) {

        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        checkNotNull(arg2);

        CountDownLatch latch = new CountDownLatch(arg0.size());
        HashMap<String, EntryProcessorResult<T>> map = new HashMap<>();
        BulkInvocationProcedureCallCallback bpcc = new BulkInvocationProcedureCallCallback(latch);
        VoltTable params = VoltParameterWrangler.convertToVoltTable(arg2);

        Iterator<?> it = arg0.iterator();

        while (it.hasNext()) {

            String key = (String) it.next();

            try {
                c.callProcedure(bpcc, "Invoke", key, cacheName, arg1.getClass().getName(), params);
            } catch (IOException e) {
                throw new CacheException("IOException:" + e.getMessage());
            }

        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new CacheException("InterruptedException:" + e.getMessage());
        }

        it = arg0.iterator();

        while (it.hasNext()) {

            String key = (String) it.next();

            EntryProcessorResult<T> result = null;

            result = bpcc.getResults().get(key);

            if (result != null) {
                map.put(key, result);
            }
        }

        return map;
    }

    @Override
    public boolean isClosed() {

        if (c == null) {
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Entry<String, byte[]>> iterator() {
        // We're being asked to return the entire database. 
        // We may throw a CacheException if the contents are too big.

        Set<Entry<String, byte[]>> answer = null;

        Object[] params = { cacheName };
        answer = (Set<Entry<String, byte[]>>) callVoltDBProc("Iterator", true, params);
        return answer.iterator();

    }

    @Override
    public void loadAll(Set<? extends String> arg0, boolean arg1, CompletionListener arg2) {

        // NOOP - we don't actually cache anything...
        arg2.onCompletion();

    }

    @Override
    public void put(String arg0, byte[] arg1) {

        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        callVoltDBProc("Put", arg0, cacheName, arg1);

    }

    @Override
    public void putAll(Map<? extends String, ? extends byte[]> arg0) {

        checkNotClosed();

        checkNotNull(arg0);

        CountDownLatch latch = new CountDownLatch(arg0.size());
        BulkProcedureCallCallback bpcc = new BulkProcedureCallCallback(latch);

        Iterator<?> it = arg0.entrySet().iterator();
        while (it.hasNext()) {

            @SuppressWarnings("rawtypes")
            Map.Entry pair = (Map.Entry) it.next();
            it.remove();

            try {
                c.callProcedure(bpcc, "Put", pair.getKey(), cacheName, pair.getValue());
            } catch (IOException e) {
                throw new CacheException("IOException:" + e.getMessage());
            }

        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new CacheException("Interrupted while waiting for response");
        }

        if (bpcc.getFailedBecause() != null) {
            throw new CacheException(bpcc.getFailedBecause());
        }

    }

    @Override
    public boolean putIfAbsent(String arg0, byte[] arg1) {

        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        Object existingKey = callVoltDBProc("PutIfAbsent", arg0, cacheName, arg1);

        if (existingKey == null) {
            return true;
        }

        return false;

    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<String, byte[]> arg0) {

        checkNotClosed();

        checkNotNull(arg0);

        if (celc == null) {
            celc = arg0;
            setEvents(true);
            startCacheEntryListenerThread();
        } else {
            throw new IllegalArgumentException();
        }

    }

    @Override
    public boolean remove(String arg0) {

        checkNotClosed();

        checkNotNull(arg0);

        Long upsertedRowCount = (Long) callVoltDBProc("Remove", arg0, cacheName);

        if (upsertedRowCount != null && upsertedRowCount.longValue() > 0) {
            return true;
        }

        return false;

    }

    @Override
    public boolean remove(String arg0, byte[] arg1) {

        checkNotClosed();

        checkNotNull(arg0);

        Long upsertedRowCount = (Long) callVoltDBProc("RemoveKeyValuePair", arg0, cacheName, arg1);

        if (upsertedRowCount != null && upsertedRowCount.longValue() > 0) {
            return true;
        }

        return false;
    }

    @Override
    public void removeAll() {

        callVoltDBProc("RemoveAll", cacheName);

    }

    @Override
    public void removeAll(Set<? extends String> arg0) {

        checkNotClosed();

        checkNotNull(arg0);

        CountDownLatch latch = new CountDownLatch(arg0.size());
        BulkProcedureCallCallback bpcc = new BulkProcedureCallCallback(latch);

        Iterator<?> it = arg0.iterator();

        while (it.hasNext()) {

            String key = (String) it.next();

            try {
                c.callProcedure(bpcc, "Remove", key, cacheName);
            } catch (IOException e) {
                throw new CacheException("IOException:" + e.getMessage());
            }

        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new CacheException("InterruptedException:" + e.getMessage());
        }

    }

    @Override
    public boolean replace(String arg0, byte[] arg1) {

        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        Object upsertedRowCount = callVoltDBProc("Replace", arg0, cacheName, arg1);

        if (upsertedRowCount != null) {
            return true;
        }

        return false;
    }

    @Override
    public boolean replace(String arg0, byte[] arg1, byte[] arg2) {

        checkNotClosed();

        checkNotNull(arg0);

        checkNotNull(arg1);

        checkNotNull(arg2);

        Object upsertedRowCount = callVoltDBProc("ReplaceKeyValuePair", arg0, cacheName, arg1, arg2);

        if (upsertedRowCount != null) {
            return true;
        }

        return false;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        throw new IllegalArgumentException("Unwrapping to class is not supported: " + arg0);
    }

    private void startCacheEntryListenerThread() {

        String[] hostnameArray = hostnames.split(",");
        StringBuffer hosts = new StringBuffer();

        for (int i = 0; i < hostnameArray.length; i++) {
            if (i > 0) {
                hosts.append(',');
            }
            hosts.append(hostnameArray[i]);
            hosts.append(':');
            hosts.append(kafkaPort);
        }

        eventConsumer = new CacheEventConsumer(getName(), hosts.toString(), celc, this);
        eventConsumerRunner = new Thread(eventConsumer);
        eventConsumerRunner.start();

    }

    private void stopCacheEntryListenerThread() {

        if (eventConsumer != null) {
            eventConsumer.stop();
            eventConsumerRunner.stop();
            eventConsumerRunner = null;
            eventConsumer = null;

        }

    }

    private Object callVoltDBProc(String procedureName, Object... params) {
        return callVoltDBProc(procedureName, false, params);
    }

    @SuppressWarnings("unchecked")
    private Object callVoltDBProc(String procedureName, boolean wantKVData, Object... params) {

        Object answer = null;
        String errorStatus = null;

        for (int i = 0; i < retryAttempts; i++) {

            // See if we can issue our call

            ClientResponse cr;
            try {
                cr = c.callProcedure(procedureName, params);

                if (cr.getStatus() == ClientResponse.SUCCESS) {
                    VoltTable[] resultsTables = cr.getResults();
                    if (resultsTables.length > 0 && resultsTables[resultsTables.length - 1].getRowCount() > 0) {

                        if (wantKVData) {

                            answer = new HashSet<Entry<String, byte[]>>(
                                    resultsTables[resultsTables.length - 1].getRowCount());

                            while (resultsTables[resultsTables.length - 1].advanceRow()) {

                                String k = resultsTables[resultsTables.length - 1].getString("k");
                                byte[] v = resultsTables[resultsTables.length - 1].getVarbinary("v");

                                KVEntry newEntry = new KVEntry(k, v);

                                ((HashSet<Entry<String, byte[]>>) answer).add(newEntry);
                            }

                        } else {
                            resultsTables[resultsTables.length - 1].advanceRow();
                            answer = resultsTables[resultsTables.length - 1].get(0);
                        }
                    }

                    errorStatus = null;
                    break;

                } else {
                    errorStatus = cr.getStatusString();
                }

            } catch (IOException e) {

                errorStatus = e.getClass().getName() + ":" + e.getMessage();
                msg(errorStatus);

            } catch (ProcCallException e) {

                errorStatus = e.getClass().getName() + ":" + e.getMessage();
                msg(errorStatus);

                if (e.getMessage().startsWith(
                        "VOLTDB ERROR: SQL ERROR Output from SQL stmt overflowed output/network buffer of 50mb")) {
                    // This is non-recoverable...don't retry...
                    throw new CacheException(TOO_MUCH_DATA_REQUESTED);
                }
            }

            long delayMs = getDelay(i);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
            }

        }

        if (errorStatus != null) {
            throw new CacheException(errorStatus);

        }

        return answer;
    }

    private long getDelay(int i) {

        return 1000 * ((long) Math.pow((i + 1), retryPower));
    }

    private Client getClient() throws Exception {

        if (c == null) {
            c = connectVoltDB(hostnames);
        }

        return c;
    }

    private void checkNotNull(Object arg0) {
        if (arg0 == null) {
            throw new NullPointerException();
        }
    }

    private void checkNotClosed() {
        if (isClosed()) {
            throw new CacheException("Cache closed");
        }
    }

    /**
     * 
     * Connect to VoltDB using native APIS
     * 
     * @param commaDelimitedHostnames
     * @return
     * @throws Exception
     */
    private static Client connectVoltDB(String commaDelimitedHostnames) throws Exception {
        Client client = null;
        ClientConfig config = null;

        try {
            VoltDBCache.msg("Logging into VoltDB");

            config = new ClientConfig(); // "admin", "idontknow");
            config.setTopologyChangeAware(true);
            config.setReconnectOnConnectionLoss(true);

            client = ClientFactory.createClient(config);

            String[] hostnameArray = commaDelimitedHostnames.split(",");

            for (int i = 0; i < hostnameArray.length; i++) {
                msg("Connect to " + hostnameArray[i] + "...");
                try {
                    client.createConnection(hostnameArray[i]);
                } catch (Exception e) {
                    msg(e.getMessage());
                }
            }

            if (client.getConnectedHostList().size() == 0) {
                throw new Exception("No hosts usable...");
            }

            msg("Connected to VoltDB");

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("VoltDB connection failed.." + e.getMessage(), e);
        }

        return client;

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

    /**
     * @return the events
     */
    public boolean getEvents() {
        return events;
    }

    /**
     * 
     * Turn on or disable change data capture
     * <p>
     * WARNING: Just because you called this doesn't mean It will stick.
     * <p>
     * Somebody else could also call it and undo what you did...
     * 
     * @param enables or disables change data capture
     */
    public void setEvents(boolean events) {
        this.events = events;
        if (events) {
            callVoltDBProc("kv_parameters.UPSERT", cacheName, "ENABLE_EVENTS", 1);
        } else {
            callVoltDBProc("kv_parameters.UPSERT", cacheName, "ENABLE_EVENTS", 0);
        }

    }

}
