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

import java.util.ArrayList;
import java.util.Date;

import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

public class VoltParameterWrangler {

    public static VoltTable convertToVoltTable(Object[] objectArray) {

        VoltTable t = new VoltTable(new VoltTable.ColumnInfo("BIGINT_COL", VoltType.BIGINT),
                new VoltTable.ColumnInfo("BIGINT_FLOAT", VoltType.FLOAT),
                new VoltTable.ColumnInfo("TIMESTAMP_COL", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("STRING_COL", VoltType.STRING),
                new VoltTable.ColumnInfo("VARBINARY_COL", VoltType.VARBINARY));

        if (objectArray == null) {
            return null;
        }

        for (int i = 0; i < objectArray.length; i++) {
            if (objectArray[i] instanceof Long || objectArray[i] instanceof Integer) {
                t.addRow(objectArray[i], null, null, null, null);
            } else if (objectArray[i] instanceof Double || objectArray[i] instanceof Float) {
                t.addRow(null, objectArray[i], null, null, null);
            } else if (objectArray[i] instanceof TimestampType || objectArray[i] instanceof Date) {
                t.addRow(null, null, objectArray[i], null, null);
            } else if (objectArray[i] instanceof String) {
                t.addRow(null, null, null, objectArray[i], null);
            } else if (objectArray[i] instanceof byte[]) {
                t.addRow(null, null, null, null, objectArray[i]);
            } else {
                t.addRow(null, null, null, null, null);
            }
        }

        return t;
    }

    public static Object[] convertFromVoltTable(VoltTable t) {

        if (t == null) {
            return null;
        }

        ArrayList<Object> paramArrayList = new ArrayList<Object>(t.getRowCount());

        while (t.advanceRow()) {

            Object tempObject = null;

            tempObject = t.getLong("BIGINT_COL");
            if (!t.wasNull()) {
                paramArrayList.add(tempObject);
            } else {
                tempObject = t.getDouble("BIGINT_FLOAT");
                if (!t.wasNull()) {
                    paramArrayList.add(tempObject);
                } else {
                    tempObject = t.getTimestampAsTimestamp("TIMESTAMP_COL");
                    if (tempObject != null) {
                        paramArrayList.add(tempObject);
                    } else {
                        tempObject = t.getString("STRING_COL");
                        if (tempObject != null) {
                            paramArrayList.add(tempObject);
                        } else {
                            tempObject = t.getVarbinary("VARBINARY_COL");
                            if (tempObject != null) {
                                paramArrayList.add(tempObject);
                            } else {

                                paramArrayList.add(null);

                            }
                        }
                    }
                }
            }
        }

        Object[] params = new Object[paramArrayList.size()];
        params = paramArrayList.toArray(params);
        return params;

    }

}
