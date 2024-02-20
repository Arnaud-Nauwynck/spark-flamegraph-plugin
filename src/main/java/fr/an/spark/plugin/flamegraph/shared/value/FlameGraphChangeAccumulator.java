package fr.an.spark.plugin.flamegraph.shared.value;

import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest.FlameGraphEntryAccumulatorChange;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest.FlameGraphThreadGroupAccumulatorChange;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureEntry;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureKey;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import fr.an.spark.plugin.flamegraph.shared.utils.LsUtils;
import lombok.Getter;
import lombok.val;

import java.util.*;

@Getter
public class FlameGraphChangeAccumulator {

    public final String name;

    protected final Map<Integer, FlameGraphEntryChangeAccumulator> entryAccumulators = new HashMap<>();

    //---------------------------------------------------------------------------------------------

    public FlameGraphChangeAccumulator(String name) {
        this.name = name;
    }

    //---------------------------------------------------------------------------------------------

    public CompactFlameGraphValue toImmutableCompactValue(FlameGraphSignatureRegistry registry) {
        SortedSet<Integer> entryIds = new TreeSet<Integer>(entryAccumulators.keySet());
        FlameGraphSignatureKey setKey = FlameGraphSignatureKey.fromIds(entryIds);
        FlameGraphSignatureEntry setEntry = registry.findOrRegister(setKey);
        int entrySetLength = setKey.length();
        int[] entryValues = new int[entrySetLength];
        for(int i = 0; i < entrySetLength; i++) {
            int stackEntryId = setKey.at(i);
            val acc = entryAccumulators.get(stackEntryId);
            if (acc == null) {
                continue; // should not occur
            }
            entryValues[i] = acc.value;
        }
        return new CompactFlameGraphValue(setEntry.id, entryValues);
    }

    public void addToStack(StackTraceEntry entry, int millis) {
        getOrCreateEntryAccumulator(entry).addValue(millis);
    }

    private FlameGraphEntryChangeAccumulator getOrCreateEntryAccumulator(StackTraceEntry entry) {
        return entryAccumulators.computeIfAbsent(entry.id, k -> new FlameGraphEntryChangeAccumulator(entry));
    }

    public FlameGraphThreadGroupAccumulatorChange createAccChange() {
        val changes = LsUtils.flatMapNonNull(entryAccumulators.values(), x -> x.toEntryChangeOrNull());
        if (changes.isEmpty()) {
            return null;
        }
        return new FlameGraphThreadGroupAccumulatorChange(name, changes);
    }

    public void applyChange(FlameGraphThreadGroupAccumulatorChange change,
                            long syncTime) {
        val childChanges = change.changes;
        if (childChanges == null) {
            return;
        }
        for (FlameGraphEntryAccumulatorChange childChange : childChanges) {
            val childAcc = entryAccumulators.get(childChange.entryId);
            if (childAcc == null) {
                continue; // should not occur, but ok
            }
            childAcc.applyChange(syncTime, childChange.valueIncr);
        }
    }


    //---------------------------------------------------------------------------------------------

    public static class FlameGraphEntryChangeAccumulator {
        public final StackTraceEntry entry;
        protected int value;

        @Getter
        protected long lastUpdatedTime;
        @Getter
        protected long lastUpdatedValue;

        public FlameGraphEntryChangeAccumulator(StackTraceEntry entry) {
            this.entry = Objects.requireNonNull(entry);
        }

        public void addValue(int millis) {
            this.value += millis;
        }

        public void applyChange(long lastTime, int lastValue) {
            if (this.lastUpdatedValue != lastValue) {
                this.lastUpdatedTime = lastTime;
                this.lastUpdatedValue = lastValue;
            }
        }

        public FlameGraphEntryAccumulatorChange toEntryChangeOrNull() {
            if (value == lastUpdatedValue) {
                return null;
            }
            return new FlameGraphEntryAccumulatorChange(entry.id, value);
        }

    }

}
