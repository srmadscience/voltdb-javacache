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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.cache.CacheException;
import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcedureCallback;

public class BulkGetCallback implements ProcedureCallback {

    CountDownLatch latch;
    Map<String, byte[]> results = null;

    public BulkGetCallback(CountDownLatch latch, Map<String, byte[]> results) {

        super();
        this.latch = latch;
        this.results = results;

    }

    @Override
    public void clientCallback(ClientResponse arg0) throws Exception {

        if (arg0.getStatus() != ClientResponse.SUCCESS) {
            throw new CacheException(arg0.getStatusString());

        } else {

            VoltTable resultsTable = arg0.getResults()[0];

            if (resultsTable.advanceRow()) {
                String key = resultsTable.getString("k");
                byte[] value = resultsTable.getVarbinary("v");
                results.put(key, value);
            }

        }

        latch.countDown();

    }

}
