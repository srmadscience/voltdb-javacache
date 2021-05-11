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

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class RemoveAll extends VoltProcedure {

    // @formatter:off
    
    public static final SQLStmt getParam = new SQLStmt(
            "SELECT param_value FROM kv_parameters WHERE param_name = ?;");

    public static final SQLStmt removeAll = new SQLStmt(
            "DELETE FROM kv WHERE c = ?;");

    public static final SQLStmt exportAll = new SQLStmt(
            "INSERT INTO kv_deltas (c,k,v,event_type) SELECT c,k,v,'" + AbstractEventTrackingProcedure.REMOVED + "' event_type FROM kv WHERE c = ? ORDER BY k ;");

 	// @formatter:on

    public VoltTable[] run(String c) throws VoltAbortException {

        voltQueueSQL(getParam, "ENABLE_EVENTS");
        VoltTable eventsTable = voltExecuteSQL()[0];

        if (eventsTable.advanceRow() && eventsTable.getLong("param_value") == 1) {
            voltQueueSQL(exportAll, c);
        }

        voltQueueSQL(removeAll, c);

        return voltExecuteSQL(true);

    }
}
