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

public abstract class AbstractEventTrackingProcedure extends VoltProcedure {

    // @formatter:off

	public static final SQLStmt getParam = new SQLStmt(
			"SELECT param_value FROM kv_parameters WHERE c = ? AND param_name = ?;");

    public static final SQLStmt exportEvent = new SQLStmt(
            "INSERT INTO kv_deltas\n"
            + "(c,k,v,event_type)\n"
            + "VALUES \n"
            + "(?,?,?,?);");
    
 // @formatter:on

    public static String CREATED = "C";
    public static String EXPIRED = "X";
    public static String UPDATED = "U";
    public static String REMOVED = "D";

    public static final byte OK = 0;
    public static final byte BAD_CLASSNAME = -1;
    public static final byte BAD_CONSTRUCTOR_NSM = -2;
    public static final byte BAD_CONSTRUCTOR_SECURITY = -3;
    public static final byte BAD_NEWINSTANCE_INSTANTIATE = -4;
    public static final byte BAD_NEWINSTANCE_ACCESS = -5;
    public static final byte BAD_NEWINSTANCE_ARGUMENT = -6;
    public static final byte BAD_NEWINSTANCE_TARGET = -7;

    protected void reportEvent(String c, String k, byte[] v, String eventType) {
        voltQueueSQL(getParam, c, "ENABLE_EVENTS");
        VoltTable[] results = voltExecuteSQL();

        if (results[results.length - 1].advanceRow() && results[results.length - 1].getLong("param_value") == 1) {
            voltQueueSQL(exportEvent, c, k, v, eventType);
            voltExecuteSQL();
        }

    }

    public VoltTable[] run() throws VoltAbortException {

        return null;

    }
}
