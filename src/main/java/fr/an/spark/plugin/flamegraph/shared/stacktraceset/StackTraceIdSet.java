package fr.an.spark.plugin.flamegraph.shared.stacktraceset;

import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import lombok.val;

import java.util.TreeSet;

public class StackTraceIdSet {
    private TreeSet<Integer> sortedEntryIds = new TreeSet<>();

    public void add(StackTraceEntry entry) {
        sortedEntryIds.add(entry.id);
    }
    public void add(int entryId) {
        sortedEntryIds.add(entryId);
    }

    public StackTraceIdSetKey toKey() {
        return StackTraceIdSetKey.fromIds(sortedEntryIds);
    }

    public StackTraceIdSet union(StackTraceIdSet other) {
        StackTraceIdSet res = new StackTraceIdSet();
        for(val x : sortedEntryIds) {
            res.add(x);
        }
        for(val x : other.sortedEntryIds) {
            res.add(x);
        }
        return res;
    }

}
