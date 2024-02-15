package fr.an.spark.plugin.flamegraph.shared.signature;

import lombok.val;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for mapping a Set of StackTraceEntries as a compact id
 *
 * used to stored in a compact way a FlameGraph as { int setId; int[] values; }
 */
public class FlameGraphSignatureRegistry {

    private final Object lock = new Object();
    @GuardedBy("lock")
    private final Map<FlameGraphSignatureKey, FlameGraphSignatureEntry> byKey = new HashMap<>();

    @GuardedBy("lock")
    private final Map<Integer, FlameGraphSignatureEntry> byId = new HashMap<>();

    @GuardedBy("lock")
    private int idGenerator = 1;

    //---------------------------------------------------------------------------------------------

    public FlameGraphSignatureRegistry() {
    }

    //---------------------------------------------------------------------------------------------

    public FlameGraphSignatureEntry findOrRegister(FlameGraphSignatureBuilder set) {
        return findOrRegister(set.toKey());
    }

    public FlameGraphSignatureEntry findOrRegister(FlameGraphSignatureKey key) {
        synchronized(lock) {
            return byKey.computeIfAbsent(key, k -> {
                val res = new FlameGraphSignatureEntry(idGenerator++, k);
                byId.put(res.id, res);
                return res;
            });
        }
    }

    public List<FlameGraphSignatureEntry> getRegisteredList() {
        synchronized(lock) {
            return new ArrayList<>(byKey.values());
        }
    }

    public FlameGraphSignatureEntry getById(int id) {
        synchronized(lock) {
            val res = byId.get(id);
            if (res == null) {
                throw new IllegalStateException("StackTraceIdSetEntry not found by id:" + id);
            }
            return res;
        }
    }

}
