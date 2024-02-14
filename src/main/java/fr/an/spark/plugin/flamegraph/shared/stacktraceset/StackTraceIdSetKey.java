package fr.an.spark.plugin.flamegraph.shared.stacktraceset;

import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import lombok.val;

import java.io.Serializable;
import java.util.Arrays;
import java.util.SortedSet;

/**
 * Key value object for Set<StackTraceEntry>
 */
public class StackTraceIdSetKey implements Serializable {
    private final int[] sortedEntryIds;

    //---------------------------------------------------------------------------------------------

    private StackTraceIdSetKey(int[] sortedEntryIds) {
        this.sortedEntryIds = sortedEntryIds;
    }

    public static StackTraceIdSetKey fromIds(SortedSet<Integer> entries) {
        int[] data = new int[entries.size()];
        int i = 0;
        for(val e : entries) {
            data[i++] = e;
        }
        return new StackTraceIdSetKey(data);
    }

    public static StackTraceIdSetKey fromEntries(SortedSet<StackTraceEntry> entries) {
        int[] data = new int[entries.size()];
        int i = 0;
        for(val e : entries) {
            data[i++] = e.id;
        }
        return new StackTraceIdSetKey(data);
    }


    //---------------------------------------------------------------------------------------------

    public int length() {
        return sortedEntryIds.length;
    }

    public int at(int pos) {
        return sortedEntryIds[pos];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackTraceIdSetKey that = (StackTraceIdSetKey) o;
        return Arrays.equals(sortedEntryIds, that.sortedEntryIds);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sortedEntryIds);
    }

}
