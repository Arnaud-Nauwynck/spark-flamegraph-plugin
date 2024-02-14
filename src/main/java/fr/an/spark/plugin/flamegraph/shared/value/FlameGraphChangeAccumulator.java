package fr.an.spark.plugin.flamegraph.shared.value;

import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest.FlameGraphEntryAccumulatorChange;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest.FlameGraphThreadGroupAccumulatorChange;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.utils.LsUtils;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public class FlameGraphChangeAccumulator {
    public final String name;
    protected final Map<Integer, FlameGraphEntryChangeAccumulator> entryAccumulators = new HashMap<>();

    //---------------------------------------------------------------------------------------------

    public FlameGraphChangeAccumulator(String name) {
        this.name = name;
    }

    public void addToStack(StackTraceEntry entry, long millis) {
        getOrCreateEntryAccumulator(entry).addTopLevelMillis(millis);
    }

    private FlameGraphEntryChangeAccumulator getOrCreateEntryAccumulator(StackTraceEntry entry) {
        return entryAccumulators.computeIfAbsent(entry.id, k -> new FlameGraphEntryChangeAccumulator(entry));
    }

    public FlameGraphThreadGroupAccumulatorChange createAccChange() {
        List<FlameGraphEntryAccumulatorChange> entryChanges = LsUtils.flatMapNonNull(entryAccumulators.values(),
                x -> x.toEntryChangeOrNull());
        if (entryChanges.isEmpty()) {
            return null;
        }
        return new FlameGraphThreadGroupAccumulatorChange(name, entryChanges);
    }

    public void onResponseUpdateLastTime(FlameGraphThreadGroupAccumulatorChange change,
                                         long syncTime) {
        List<FlameGraphEntryAccumulatorChange> childChanges = change.changes;
        if (childChanges == null) {
            return;
        }
        for (FlameGraphEntryAccumulatorChange childChange : childChanges) {
            FlameGraphEntryChangeAccumulator entryAcc = entryAccumulators.get(childChange.entryId);
            if (entryAcc == null) {
                continue; // should not occur, but ok
            }
            entryAcc.onResponseUpdateLastTime(syncTime, childChange.valueIncr);
        }
    }


    //---------------------------------------------------------------------------------------------

    public static class FlameGraphEntryChangeAccumulator {
        public final StackTraceEntry entry;
        protected long topLevelSumMillis;

        @Getter
        protected long lastUpdatedTopLevelTime;
        @Getter
        protected long lastUpdatedTopLevelSumMillis;

        public FlameGraphEntryChangeAccumulator(StackTraceEntry entry) {
            this.entry = Objects.requireNonNull(entry);
        }

        public void addTopLevelMillis(long millis) {
            this.topLevelSumMillis += millis;
        }

        public void onResponseUpdateLastTime(long time, long sumMillis) {
            if (this.lastUpdatedTopLevelSumMillis != sumMillis) {
                this.lastUpdatedTopLevelTime = time;
                this.lastUpdatedTopLevelSumMillis = sumMillis;
            }
        }

        public FlameGraphEntryAccumulatorChange toEntryChangeOrNull() {
            if (topLevelSumMillis == lastUpdatedTopLevelSumMillis) {
                return null;
            }
            return new FlameGraphEntryAccumulatorChange(entry.id, topLevelSumMillis);
        }

    }

}
