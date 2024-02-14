package fr.an.spark.plugin.flamegraph.shared.stacktraceset;

import java.io.Serializable;

/**
 * registered entity (StackTraceIdSetKey) in registry StackTraceIdSetRegistry
 */
public class StackTraceIdSetEntry implements Serializable {

    public final int id;
    public final StackTraceIdSetKey entrySet;

    //---------------------------------------------------------------------------------------------

    public StackTraceIdSetEntry(int id, StackTraceIdSetKey entrySet) {
        this.id = id;
        this.entrySet = entrySet;
    }

    //---------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "stackTraceEntrySet#" + id;
    }

}
