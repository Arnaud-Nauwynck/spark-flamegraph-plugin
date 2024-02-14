package fr.an.spark.plugin.flamegraph.shared.stacktraceset;

import lombok.val;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackTraceIdSetRegistry {

    private final Object lock = new Object();
    @GuardedBy("lock")
    private final Map<StackTraceIdSetKey, StackTraceIdSetEntry> byKey = new HashMap<>();

    @GuardedBy("lock")
    private final Map<Integer, StackTraceIdSetEntry> byId = new HashMap<>();

    @GuardedBy("lock")
    private int idGenerator = 1;

    //---------------------------------------------------------------------------------------------

    public StackTraceIdSetRegistry() {
    }

    //---------------------------------------------------------------------------------------------

    public StackTraceIdSetEntry findOrRegister(StackTraceIdSet set) {
        return findOrRegister(set.toKey());
    }

    public StackTraceIdSetEntry findOrRegister(StackTraceIdSetKey key) {
        synchronized(lock) {
            return byKey.computeIfAbsent(key, k -> {
                val res = new StackTraceIdSetEntry(idGenerator++, k);
                byId.put(res.id, res);
                return res;
            });
        }
    }

    public List<StackTraceIdSetEntry> getRegisteredList() {
        synchronized(lock) {
            return new ArrayList<>(byKey.values());
        }
    }

    public StackTraceIdSetEntry getById(int id) {
        synchronized(lock) {
            val res = byId.get(id);
            if (res == null) {
                throw new IllegalStateException("StackTraceIdSetEntry not found by id:" + id);
            }
            return res;
        }
    }

}
