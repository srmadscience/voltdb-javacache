package org.voltdb.jsr107;

import javax.cache.Cache;

public class KVEntry implements Cache.Entry<String,byte[]>{

    String key;
    byte[] value;
    

    public KVEntry(String key, byte[] value) {
        super();
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        return null;
    }

}
