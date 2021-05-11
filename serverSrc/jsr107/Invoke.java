package jsr107;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;

public class Invoke extends AbstractEventTrackingProcedure {

    // @formatter:off

	public static final SQLStmt getV = new SQLStmt(
			"SELECT v FROM kv WHERE c = ? AND k = ?;");

    public static final SQLStmt upsertKV = new SQLStmt(
            "UPSERT INTO kv\n"
            + "(c,k,v)\n"
            + "VALUES \n"
            + "(?,?,?);");
    
    public static final SQLStmt deleteKV = new SQLStmt(
            "DELETE "
            + "FROM kv "
            + "WHERE c = ? AND k = ?;");
 
    
    HashMap<String,EntryProcessor<String, byte[], VoltTable[]>> processorMap = new HashMap<String,EntryProcessor<String, byte[], VoltTable[]>>();
    
 	// @formatter:on

    @SuppressWarnings("unchecked")
    public VoltTable[] run(String k, String c, String processorClassName, VoltTable paramsAsVoltTable)
            throws VoltAbortException {

        this.setAppStatusCode(OK);
        this.setAppStatusString(k);

        Object[] params = VoltParameterWrangler.convertFromVoltTable(paramsAsVoltTable);
        VoltTable[] results = null;
        MutableEntry<String, byte[]> theEntry = null;
        boolean previouslyExisted = false;

        EntryProcessor<String, byte[], VoltTable[]> ourProcessor = processorMap
                .get(c + System.lineSeparator() + processorClassName);

        if (ourProcessor == null) {

            Class<?> newClass = null;
            Constructor<?> cons = null;

            try {
                newClass = Class.forName(processorClassName);
            } catch (ClassNotFoundException e) {
                this.setAppStatusCode(BAD_CLASSNAME);
                return null;
            }

            try {
                cons = newClass.getConstructor();
            } catch (NoSuchMethodException e) {
                this.setAppStatusCode(BAD_CONSTRUCTOR_NSM);
                return null;
            } catch (SecurityException e) {
                this.setAppStatusCode(BAD_CONSTRUCTOR_SECURITY);
                return null;
            }

            try {
                ourProcessor = (EntryProcessor<String, byte[], VoltTable[]>) cons.newInstance();
            } catch (InstantiationException e) {
                this.setAppStatusCode(BAD_NEWINSTANCE_INSTANTIATE);
                return null;
            } catch (IllegalAccessException e) {
                this.setAppStatusCode(BAD_NEWINSTANCE_ACCESS);
                return null;
            } catch (IllegalArgumentException e) {
                this.setAppStatusCode(BAD_NEWINSTANCE_ARGUMENT);
                return null;
            } catch (InvocationTargetException e) {
                this.setAppStatusCode(BAD_NEWINSTANCE_TARGET);
                return null;
            }

            processorMap.put(c + System.lineSeparator() + processorClassName, ourProcessor);

        }

        voltQueueSQL(getV, c, k);

        final VoltTable[] oldValues = voltExecuteSQL();

        if (oldValues[0].advanceRow()) {
            previouslyExisted = true;
            byte[] oldValue = oldValues[0].getVarbinary("v");
            theEntry = new VoltDBMutableEntry(k, oldValue, true);
        } else {
            theEntry = new VoltDBMutableEntry(k, null, false);
        }

        try {
            results = ourProcessor.process(theEntry, params);
        } catch (EntryProcessorException e) {
            throw new VoltAbortException(e);
        }

        if (theEntry.exists()) {
            voltQueueSQL(upsertKV, c, k, theEntry.getValue());
            reportEvent(c, k, theEntry.getValue(), UPDATED);
        } else if (previouslyExisted) {
            voltQueueSQL(deleteKV, c, k);
            reportEvent(c, k, theEntry.getValue(), REMOVED);
        }

        voltExecuteSQL(true);

        return results;

    }

}
