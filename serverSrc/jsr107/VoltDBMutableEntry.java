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

import javax.cache.processor.MutableEntry;

public class VoltDBMutableEntry implements MutableEntry<String, byte[]> {

    String k;
    byte[] value;
    boolean exists;

    public VoltDBMutableEntry(String k, byte[] value, boolean exists) {
        this.k = k;
        this.value = value;
        this.exists = exists;
    }

    @Override
    public String getKey() {
        return k;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        throw new IllegalArgumentException("Unsupported");
    }

    @Override
    public boolean exists() {

        return exists;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    @Override
    public void remove() {
        exists = false;

    }

    @Override
    public void setValue(byte[] arg0) {
        this.value = arg0;

    }

}
