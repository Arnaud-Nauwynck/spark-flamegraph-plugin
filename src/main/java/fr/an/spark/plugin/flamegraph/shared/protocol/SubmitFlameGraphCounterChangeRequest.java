package fr.an.spark.plugin.flamegraph.shared.protocol;

import fr.an.spark.plugin.flamegraph.shared.FlameGraphThreadGroupsChangeAccumulator;
import fr.an.spark.plugin.flamegraph.shared.utils.LsUtils;
import fr.an.spark.plugin.flamegraph.shared.value.FlameGraphChangeAccumulator;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
public class SubmitFlameGraphCounterChangeRequest implements Serializable {

    public final String executorId;
    public final List<FlameGraphThreadGroupAccumulatorChange> changes;

    @RequiredArgsConstructor
    public static class FlameGraphThreadGroupAccumulatorChange implements Serializable {
        public final String threadGroupName;
        public final List<FlameGraphEntryAccumulatorChange> changes;
    }

    @RequiredArgsConstructor
    public static class FlameGraphEntryAccumulatorChange implements Serializable {
        public final int entryId;
        public final int valueIncr;
    }

}
