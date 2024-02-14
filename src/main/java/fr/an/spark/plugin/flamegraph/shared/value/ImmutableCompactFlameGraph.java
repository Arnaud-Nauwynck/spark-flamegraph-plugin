package fr.an.spark.plugin.flamegraph.shared.value;

import fr.an.spark.plugin.flamegraph.shared.stacktraceset.StackTraceIdSetRegistry;

import java.io.Serializable;

public class ImmutableCompactFlameGraph implements Serializable {

    // private transient StackTraceIdSetRegistry stackTraceRegistry;
    private final int entrySetId;
    private final int[] entryValues;

    //---------------------------------------------------------------------------------------------

    private ImmutableCompactFlameGraph(int entrySetId, int[] entryValues) {
        this.entrySetId = entrySetId;
        this.entryValues = entryValues;
    }

    //---------------------------------------------------------------------------------------------


}
