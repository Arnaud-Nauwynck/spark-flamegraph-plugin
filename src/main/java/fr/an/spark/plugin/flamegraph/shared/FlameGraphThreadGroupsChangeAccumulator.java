package fr.an.spark.plugin.flamegraph.shared;

import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeResponse;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.utils.LsUtils;
import fr.an.spark.plugin.flamegraph.shared.value.FlameGraphChangeAccumulator;
import lombok.Getter;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

public class FlameGraphThreadGroupsChangeAccumulator {

    // TOADD public final String executorId;

    @Getter
    protected final Map<String, FlameGraphChangeAccumulator> threadGroupAccumulators = new HashMap<>();

    //---------------------------------------------------------------------------------------------

    public FlameGraphThreadGroupsChangeAccumulator() {
    }

    //---------------------------------------------------------------------------------------------

    public void addToThreadGroupStackEntry(String threadGroupName, StackTraceEntry entry, int incrValue) {
        FlameGraphChangeAccumulator threadGroupCounter = getOrCreateThreadGroup(threadGroupName);
        threadGroupCounter.addToStack(entry, incrValue);
    }

    public FlameGraphChangeAccumulator getOrCreateThreadGroup(String threadGroupName) {
        return threadGroupAccumulators.computeIfAbsent(threadGroupName, k -> new FlameGraphChangeAccumulator(k));
    }

    public SubmitFlameGraphCounterChangeRequest createChangeRequest(String executorId) {
        val changes = LsUtils.flatMapNonNull(threadGroupAccumulators.values(), x -> x.createAccChange());
        return new SubmitFlameGraphCounterChangeRequest(executorId, changes);
    }

    public void onResponseUpdateLastTime(
            SubmitFlameGraphCounterChangeRequest change,
            SubmitFlameGraphCounterChangeResponse resp,
            long syncTime) {
        for(val childChange : change.changes) {
            FlameGraphChangeAccumulator childAccumulator = threadGroupAccumulators.get(childChange.threadGroupName);
            if (childAccumulator == null) {
                continue; // should not occur, but ok
            }
            childAccumulator.onResponseUpdateLastTime(childChange, syncTime);
        }
    }

}
