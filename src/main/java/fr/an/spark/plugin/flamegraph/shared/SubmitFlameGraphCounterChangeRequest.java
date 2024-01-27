package fr.an.spark.plugin.flamegraph.shared;

import fr.an.spark.plugin.flamegraph.shared.FlameGraphAccumulator.FlameGraphEntryAccumulator;
import fr.an.spark.plugin.flamegraph.shared.FlameGraphAccumulator.FlameGraphThreadGroupAccumulator;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

@RequiredArgsConstructor
public class SubmitFlameGraphCounterChangeRequest implements Serializable {

    public final String executorId;
    public final List<FlameGraphThreadGroupAccumulatorChange> threadGroupAccumulatorChanges;

    @RequiredArgsConstructor
    public static class FlameGraphThreadGroupAccumulatorChange implements Serializable {
        public final String threadGroupName;
        public final List<FlameGraphEntryAccumulatorChange> entryAccumulators;
    }

    @RequiredArgsConstructor
    public static class FlameGraphEntryAccumulatorChange implements Serializable {
        public final int entryId;
        public final long topLevelSumMillis;

    }

    public static SubmitFlameGraphCounterChangeRequest createChangeRequest(String executorId, FlameGraphAccumulator src) {
        val threadGroupAccumulatorChanges = LsUtils.mapNonNull(src.getThreadGroupAccumulators().entrySet(),
                x -> createThreadGroupAccChange(x.getKey(), x.getValue()));
        return new SubmitFlameGraphCounterChangeRequest(executorId, threadGroupAccumulatorChanges);
    }

    private static FlameGraphThreadGroupAccumulatorChange createThreadGroupAccChange(String threadGroupName, FlameGraphThreadGroupAccumulator src) {
        List<FlameGraphEntryAccumulatorChange> entryChanges = LsUtils.mapNonNull(src.getEntryAccumulators().entrySet(),
                x -> createEntryChange(x.getKey(), x.getValue()));
        if (entryChanges.isEmpty()) {
            return null;
        }
        return new FlameGraphThreadGroupAccumulatorChange(threadGroupName, entryChanges);
    }

    private static FlameGraphEntryAccumulatorChange createEntryChange(int entryId, FlameGraphEntryAccumulator src) {
        if (src.topLevelSumMillis == src.lastUpdatedTopLevelSumMillis) {
            return null;
        }
        return new FlameGraphEntryAccumulatorChange(entryId, src.topLevelSumMillis);
    }

}
