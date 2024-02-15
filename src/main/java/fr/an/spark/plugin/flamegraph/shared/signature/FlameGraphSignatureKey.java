package fr.an.spark.plugin.flamegraph.shared.signature;

import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import lombok.val;

import java.io.Serializable;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Key value object for Set<StackTraceEntry>
 */
public class FlameGraphSignatureKey implements Serializable {
    private final int[] sortedEntryIds;

    //---------------------------------------------------------------------------------------------

    private FlameGraphSignatureKey(int[] sortedEntryIds) {
        this.sortedEntryIds = sortedEntryIds;
    }

    public static FlameGraphSignatureKey fromIds(SortedSet<Integer> entries) {
        int[] data = new int[entries.size()];
        int i = 0;
        for(val e : entries) {
            data[i++] = e;
        }
        return new FlameGraphSignatureKey(data);
    }

    public static FlameGraphSignatureKey fromEntries(SortedSet<StackTraceEntry> entries) {
        int[] data = new int[entries.size()];
        int i = 0;
        for(val e : entries) {
            data[i++] = e.id;
        }
        return new FlameGraphSignatureKey(data);
    }


    //---------------------------------------------------------------------------------------------

    public int length() {
        return sortedEntryIds.length;
    }

    public int at(int pos) {
        return sortedEntryIds[pos];
    }

    public int nextIndexOf(int id, int prevIndex) {
        int i = prevIndex;
        for(; i < sortedEntryIds.length && sortedEntryIds[i] < id; i++) {
        }
        return i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlameGraphSignatureKey that = (FlameGraphSignatureKey) o;
        return Arrays.equals(sortedEntryIds, that.sortedEntryIds);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sortedEntryIds);
    }

    //---------------------------------------------------------------------------------------------

    public FlameGraphSignatureKey union(FlameGraphSignatureKey other) {
        // TODO.. rewrite with "sorted merge" algorithm, 2 phases for count array length, then fill array
        TreeSet<Integer> tmp = new TreeSet<>();
        for (val id: sortedEntryIds) tmp.add(id);
        for (val id: other.sortedEntryIds) tmp.add(id);
        int len = tmp.size();
        int[] unionIds = new int[len];
        int i = 0;
        for(val id: tmp) {
            unionIds[i++] = id;
        }
        return new FlameGraphSignatureKey(unionIds);
    }


}
