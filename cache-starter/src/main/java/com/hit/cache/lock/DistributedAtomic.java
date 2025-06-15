package com.hit.cache.lock;

public interface DistributedAtomic {

    boolean setIfAbsent(String key, String val, long ttl);

    boolean setIfAbsentWithSuffix(String key, String val, long ttl, String suffix);

    Boolean deleteKeyVal(String key, String val);

}
