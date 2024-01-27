package fr.an.spark.plugin.flamegraph.shared;

import lombok.Getter;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FlameGraphAccumulator {

    public final StackTraceEntry rootEntry;

    @Getter
    public final Map<String, FlameGraphThreadGroupAccumulator> threadGroupAccumulators = new HashMap<>();

    //---------------------------------------------------------------------------------------------

    public FlameGraphAccumulator(StackTraceEntry rootEntry) {
        this.rootEntry = Objects.requireNonNull(rootEntry);
    }

    //---------------------------------------------------------------------------------------------

    public FlameGraphThreadGroupAccumulator getOrCreateThreadGroup(String threadGroupName) {
        return threadGroupAccumulators.computeIfAbsent(threadGroupName,
                k -> new FlameGraphThreadGroupAccumulator(this, k));
    }

    //---------------------------------------------------------------------------------------------

    @Getter
    public static class FlameGraphThreadGroupAccumulator {
        public final FlameGraphAccumulator owner;
        public final String threadGroupName;
        protected final Map<Integer, FlameGraphEntryAccumulator> entryAccumulators = new HashMap<>();

        //---------------------------------------------------------------------------------------------

        public FlameGraphThreadGroupAccumulator(FlameGraphAccumulator flameGraph, String threadGroupName) {
            this.owner = Objects.requireNonNull(flameGraph);
            this.threadGroupName = Objects.requireNonNull(threadGroupName);
        }

        public void accumulateResolvedStackTrace(StackTraceEntry entry, long millis) {
            getOrCreateEntryCounter(entry).addTopLevelMillis(millis);
        }

        private FlameGraphEntryAccumulator getOrCreateEntryCounter(StackTraceEntry entry) {
            return entryAccumulators.computeIfAbsent(entry.id,
                    k -> new FlameGraphEntryAccumulator(this, entry));
        }

        /*pp*/ void updateAllSumMillis() {
            for (val entryCounter : entryAccumulators.values()) {
                entryCounter.updateAllParentStackElementSumMillis();
            }
        }

    }

    //---------------------------------------------------------------------------------------------

    public static class FlameGraphEntryAccumulator {
        public final FlameGraphThreadGroupAccumulator owner;
        public final StackTraceEntry entry;
//        protected final FlameGraphEntryAccumulator parent;
        protected long topLevelSumMillis;


        protected long lastUpdatedTopLevelTime;
        protected long lastUpdatedTopLevelSumMillis;

        // slow recomputable by scanning parent->parent->.. of ALL topLevelEntries
//        protected long synthethizedSumMillis;
//
//        protected final Map<Integer, FlameGraphEntryAccumulator> synthetizedChildCounters = new HashMap<>();

        public FlameGraphEntryAccumulator(FlameGraphThreadGroupAccumulator threadGroupCounter, StackTraceEntry entry) {
            this.owner = Objects.requireNonNull(threadGroupCounter);
            this.entry = Objects.requireNonNull(entry);
//            this.parent = (entry.parent != null)?
//                    threadGroupCounter.getOrCreateEntryCounter(entry.parent) : null; // TODO impossible.. would throw ConcurrentModificationException
//            if (parent != null) {
//                parent.synthetizedChildCounters.put(entry.id, this);
//            }
        }

        public void addTopLevelMillis(long millis) {
            this.topLevelSumMillis += millis;
        }

        public void setLastUpdated(long time, long sumMillis) {
            if (this.lastUpdatedTopLevelSumMillis != sumMillis) {
                this.lastUpdatedTopLevelTime = time;
                this.lastUpdatedTopLevelSumMillis = sumMillis;
            }
        }

        /*pp*/ void updateAllParentStackElementSumMillis() {
            if (lastUpdatedTopLevelSumMillis != topLevelSumMillis) {
                long incr = topLevelSumMillis - lastUpdatedTopLevelSumMillis;
                this.lastUpdatedTopLevelSumMillis = topLevelSumMillis;

                // TODO !!
//                for(FlameGraphEntryAccumulator curr = this; curr != null; curr = curr.parent) {
//                    curr.synthethizedSumMillis += incr;
//                }
            }
        }

    }

}
