package fr.an.spark.plugin.flamegraph.shared;

import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeResponse;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.utils.LsUtils;
import fr.an.spark.plugin.flamegraph.shared.value.FlameGraphChangeAccumulator;
import fr.an.spark.plugin.flamegraph.shared.value.CompactFlameGraphValue;
import fr.an.spark.plugin.flamegraph.shared.value.ThreadGroupsCompactFlameGraphValue;
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
        val syncTime = System.currentTimeMillis();
        val changes = LsUtils.flatMapNonNull(threadGroupAccumulators.values(), x -> x.createAccChange());
        return new SubmitFlameGraphCounterChangeRequest(executorId, syncTime, changes);
    }

    public SubmitFlameGraphCounterChangeResponse applyChangeRequest(
            SubmitFlameGraphCounterChangeRequest changeReq
    ) {
        val reqSyncTime = changeReq.syncTime;
        val res = new SubmitFlameGraphCounterChangeResponse(); // unused yet
        for(val childChange : changeReq.changes) {
            FlameGraphChangeAccumulator childAccumulator = threadGroupAccumulators.get(childChange.threadGroupName);
            if (childAccumulator == null) {
                continue; // should not occur, but ok
            }
            childAccumulator.applyChange(childChange, reqSyncTime);
        }
        return res;
    }

    public ThreadGroupsCompactFlameGraphValue resetAndGetThreadGroupsCompactValue(FlameGraphSignatureRegistry signatureRegistry) {
        val res = new HashMap<String, CompactFlameGraphValue>();
        for(val e : threadGroupAccumulators.entrySet()) {
            val key = e.getKey();
            val acc = e.getValue();
            val elt = acc.toImmutableCompactValue(signatureRegistry);
            res.put(key, elt);
        }
        threadGroupAccumulators.clear();
        return new ThreadGroupsCompactFlameGraphValue(res);
    }

}
