package com.zipeg;

import java.util.*;
import java.io.File;

public class Cache extends LinkedHashMap {

    private static final int MAX_STORAGE = 512 * Util.MB;
    private long total = -1;
    private static Cache instance = new Cache();

    public static Cache getInstance() {
        return instance;
    }

    public boolean containsKey(Object key) {
        assert IdlingEventQueue.isDispatchThread();
        assert key instanceof File;
        return super.containsKey(key);
    }

    public Object get(Object key) {
        assert IdlingEventQueue.isDispatchThread();
        assert key instanceof File;
        return super.get(key);
    }

    public Object put(Object key, Object value) {
        assert IdlingEventQueue.isDispatchThread();
        assert key instanceof File;
        assert value != null && value instanceof CacheEntry;
        CacheEntry old = (CacheEntry)super.put(key, value);
        // if cache entry has been changed from outside it is
        // impossible to maintain the total amount of bytes in
        // cache w/o recalculating it. Luckily put() is relatively
        // rare operation.
        total = 0;
        for (Iterator i = values().iterator(); i.hasNext();) {
            CacheEntry ce = (CacheEntry)i.next();
            total += ce.temp.length();
        }
        return old;
    }

    public void clear() {
        assert IdlingEventQueue.isDispatchThread();
        // see: http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=6417205
        //      http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4724038
        System.gc();
        System.runFinalization();
        Util.sleep(1000);
        for (Iterator i = values().iterator(); i.hasNext();) {
            CacheEntry ce = (CacheEntry)i.next();
            ce.delete();
        }
        super.clear();
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
        assert IdlingEventQueue.isDispatchThread();
        if (total > MAX_STORAGE) {
            CacheEntry ce = (CacheEntry)eldest.getValue();
            ce.delete();
            return true;
        } else {
            return false;
        }
    }

}
