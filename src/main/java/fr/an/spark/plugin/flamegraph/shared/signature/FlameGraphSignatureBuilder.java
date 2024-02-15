package fr.an.spark.plugin.flamegraph.shared.signature;

import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;

import java.util.Collection;
import java.util.TreeSet;

public class FlameGraphSignatureBuilder {
    private final TreeSet<Integer> sortedEntryIds = new TreeSet<>();

    //---------------------------------------------------------------------------------------------

    public FlameGraphSignatureBuilder() {
    }

    //---------------------------------------------------------------------------------------------

    public void add(StackTraceEntry entry) {
        sortedEntryIds.add(entry.id);
    }
    public void add(int entryId) {
        sortedEntryIds.add(entryId);
    }
    public void addAll(Collection<Integer> entryIds) {
        sortedEntryIds.addAll(entryIds);
    }

    public FlameGraphSignatureKey toKey() {
        return FlameGraphSignatureKey.fromIds(sortedEntryIds);
    }

    public FlameGraphSignatureBuilder union(FlameGraphSignatureBuilder other) {
        FlameGraphSignatureBuilder res = new FlameGraphSignatureBuilder();
        res.addAll(sortedEntryIds);
        res.addAll(other.sortedEntryIds);
        return res;
    }

}
