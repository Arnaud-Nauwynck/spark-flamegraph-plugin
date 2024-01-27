package fr.an.spark.plugin.flamegraph.shared;

import fr.an.spark.plugin.flamegraph.shared.FlameGraphAccumulator.FlameGraphEntryAccumulator;
import fr.an.spark.plugin.flamegraph.shared.FlameGraphAccumulator.FlameGraphThreadGroupAccumulator;
import fr.an.spark.plugin.flamegraph.shared.SubmitFlameGraphCounterChangeRequest.FlameGraphEntryAccumulatorChange;
import fr.an.spark.plugin.flamegraph.shared.SubmitFlameGraphCounterChangeRequest.FlameGraphThreadGroupAccumulatorChange;
import lombok.val;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SubmitFlameGraphCounterChangeResponse implements Serializable {


    public static void onResponseUpdateLastTime(
            FlameGraphAccumulator src,
            SubmitFlameGraphCounterChangeRequest change,
            SubmitFlameGraphCounterChangeResponse resp,
            long syncTime) {
        Map<String, FlameGraphThreadGroupAccumulator> threadGroupAccumulators = src.getThreadGroupAccumulators();
        for(val threadGroupAccChangeReq : change.threadGroupAccumulatorChanges) {
            FlameGraphThreadGroupAccumulator threadGroupAccumulator = threadGroupAccumulators.get(threadGroupAccChangeReq.threadGroupName);
            if (threadGroupAccumulator == null) {
                continue; // should not occur, but ok
            }
            updateLastTime(threadGroupAccumulator, threadGroupAccChangeReq, syncTime);
        }
    }

    private static void updateLastTime(FlameGraphThreadGroupAccumulator src,
                                       FlameGraphThreadGroupAccumulatorChange change,
                                       long syncTime) {
        List<FlameGraphEntryAccumulatorChange> entryAccChanges = change.entryAccumulators;
        if (entryAccChanges == null) {
            return;
        }
        Map<Integer, FlameGraphEntryAccumulator> entryAccumulators = src.entryAccumulators;
        for (FlameGraphEntryAccumulatorChange entryAccChange : entryAccChanges) {
            FlameGraphEntryAccumulator entryAcc = entryAccumulators.get(entryAccChange.entryId);
            if (entryAcc == null) {
                continue; // should not occur, but ok
            }
            entryAcc.setLastUpdated(syncTime, entryAccChange.topLevelSumMillis);
        }
    }

}
