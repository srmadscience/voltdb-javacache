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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.voltdb.VoltTable;
import org.voltdb.jsr107.VoltDBCache;
import org.voltdb.types.TimestampType;

import jsr107.VoltParameterWrangler;

class VoltDBCacheParameterWranglerTest {

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
    void testVoltTable() {

        try {
            try {

                Object[] objArray = null;
                VoltTable theTable = VoltParameterWrangler.convertToVoltTable(objArray);

                if (theTable != null) {
                    fail("null not managed");
                }

                objArray = new Object[0];
                theTable = VoltParameterWrangler.convertToVoltTable(objArray);

                if (theTable.getRowCount() != 0) {
                    fail("zero length not managed");
                }

                Object[] objArray2 = { 42l };
                theTable = VoltParameterWrangler.convertToVoltTable(objArray2);
                Object[] objArray2a = VoltParameterWrangler.convertFromVoltTable(theTable);

                if (!Arrays.equals(objArray2, objArray2a)) {
                    fail("bigint");
                }

                Object[] objArray3 = { 43.4d };
                theTable = VoltParameterWrangler.convertToVoltTable(objArray3);
                Object[] objArray3a = VoltParameterWrangler.convertFromVoltTable(theTable);

                if (!Arrays.equals(objArray3, objArray3a)) {
                    fail("double");
                }

                Object[] objArray4 = { "x" };
                theTable = VoltParameterWrangler.convertToVoltTable(objArray4);
                Object[] objArray4a = VoltParameterWrangler.convertFromVoltTable(theTable);

                if (!Arrays.equals(objArray4, objArray4a)) {
                    fail("string");
                }

                Object[] objArray6 = { new TimestampType() };
                theTable = VoltParameterWrangler.convertToVoltTable(objArray6);
                Object[] objArray6a = VoltParameterWrangler.convertFromVoltTable(theTable);

                if (!Arrays.equals(objArray6, objArray6a)) {
                    fail("TimestampType");
                }

                Object[] objArray7 = { "x".getBytes() };
                theTable = VoltParameterWrangler.convertToVoltTable(objArray7);
                Object[] objArray7a = VoltParameterWrangler.convertFromVoltTable(theTable);

                if (!Arrays.deepEquals(objArray7, objArray7a)) {
                    fail("byte[]");
                }

                Object[] objArray8 = { null };
                theTable = VoltParameterWrangler.convertToVoltTable(objArray8);
                Object[] objArray8a = VoltParameterWrangler.convertFromVoltTable(theTable);

                if (!Arrays.deepEquals(objArray8, objArray8a)) {
                    fail("null");
                }

                Object[] objArray9 = { 42l, 43.4d, "Z", "Z".getBytes(), new TimestampType(), null };
                theTable = VoltParameterWrangler.convertToVoltTable(objArray9);
                Object[] objArray9a = VoltParameterWrangler.convertFromVoltTable(theTable);

                if (!Arrays.deepEquals(objArray9, objArray9a)) {
                    fail("null");
                }

            } catch (Exception e) {
                fail(e);
            }
        } catch (Exception e) {
            fail(e);
        }

    }

}
