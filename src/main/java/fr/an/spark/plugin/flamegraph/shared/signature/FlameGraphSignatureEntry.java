package fr.an.spark.plugin.flamegraph.shared.signature;

import java.io.Serializable;

/**
 * registered entity (StackTraceIdSetKey) in registry StackTraceIdSetRegistry
 */
public class FlameGraphSignatureEntry implements Serializable {

    public final int id;
    public final FlameGraphSignatureKey stackIdsKey;

    //---------------------------------------------------------------------------------------------

    public FlameGraphSignatureEntry(int id, FlameGraphSignatureKey stackIds) {
        this.id = id;
        this.stackIdsKey = stackIds;
    }

    //---------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "stackTraceSet#" + id;
    }

}
