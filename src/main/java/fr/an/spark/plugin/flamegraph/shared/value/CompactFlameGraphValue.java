package fr.an.spark.plugin.flamegraph.shared.value;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTOBuilder;
import fr.an.spark.plugin.flamegraph.shared.SumScaleValueSupport;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureEntry;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntryRegistry;
import fr.an.spark.plugin.flamegraph.shared.value.ThreadGroupsCompactFlameGraphValue.ThreadGroupsCompactFlameGraphSumScaleValueSupport;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompactFlameGraphValue implements Serializable {

    // ... implicit FlameGraphSignatureRegistry signatureRegistry;
    private final int entrySetId;
    private final int[] entryValues;

    //---------------------------------------------------------------------------------------------

    public CompactFlameGraphValue(int entrySetId,
                                  int[] entryValues // assume immutable, transfer ownership
    ) {
        this.entrySetId = entrySetId;
        this.entryValues = entryValues;
    }

    //---------------------------------------------------------------------------------------------

    @AllArgsConstructor
    public static class StackEntryIdValue {
        public final int stackEntryId;
        public final int value;
    }

    /** see also toEntryValuesArray() */
    public List<StackEntryIdValue> toEntryValues(FlameGraphSignatureRegistry signatureRegistry) {
        FlameGraphSignatureEntry signature = signatureRegistry.getById(entrySetId);
        val stackIds = signature.stackIdsKey;
        val stackCount = stackIds.length();
        val res = new ArrayList<StackEntryIdValue>(stackCount);
        for(int i = 0; i < stackCount; i++) {
            res.add(new StackEntryIdValue(stackIds.at(i), entryValues[i]));
        }
        return res;
    }

    /** @return array of interleaved stackEntryId,value: { stack[0],value[],stack[1],value[1].. } */
    public int[] toEntryValuesArray(FlameGraphSignatureRegistry signatureRegistry) {
        FlameGraphSignatureEntry signature = signatureRegistry.getById(entrySetId);
        val stackIds = signature.stackIdsKey;
        val stackCount = stackIds.length();
        int[] res = new int[2*stackCount];
        for(int i = 0; i < stackCount; i++) {
            res[2*i] = stackIds.at(i);
            res[2*i+1] = entryValues[i];
        }
        return res;
    }

    public void addTo(FlameGraphNodeDTOBuilder dtoBuilder, double coef,
                      FlameGraphSignatureRegistry signatureRegistry) {
        FlameGraphSignatureEntry signature = signatureRegistry.getById(entrySetId);
        val stackIds = signature.stackIdsKey;
        val stackCount = stackIds.length();
        val stackRegistry = signatureRegistry.stackTraceEntryRegistry;
        for(int i = 0; i < stackCount; i++) {
            StackTraceEntry stackEntry = stackRegistry.findById(stackIds.at(i));
            val addValue = entryValues[i] * coef;
            stackEntry.addTo(dtoBuilder, addValue);
        }
    }


//    StackTraceEntry stackEntry = stackRegistry.findById(stackId);
//    StackTraceEntry[] stackTrace = stackEntry.toStackTrace();
//    val stackTraceLen = stackTrace.length;
//            for(int j = 0; j < stackTraceLen; j++) {
//
//    }

    public CompactFlameGraphValue sum(CompactFlameGraphValue other, FlameGraphSignatureRegistry registry) {
        return sumOf(this, other, registry);
    }

    public static CompactFlameGraphValue sumOf(CompactFlameGraphValue left, CompactFlameGraphValue right, FlameGraphSignatureRegistry registry) {
        val leftSet = registry.getById(left.entrySetId);
        val rightSet = registry.getById(right.entrySetId);
        val leftIdsKey = leftSet.stackIdsKey;
        val rightIdsKey = rightSet.stackIdsKey;
        val unionIds = leftIdsKey.union(rightIdsKey);
        val unionSetEntry = registry.findOrRegister(unionIds);
        val unionIdsKey = unionSetEntry.stackIdsKey;
        val unionSetLength = unionIdsKey.length();
        val sumValues = new int[unionSetLength];
        val leftLength = leftIdsKey.length();
        int idx = 0;
        for(int i = 0; i < leftLength; i++) {
            int id = leftIdsKey.at(i);
            idx = unionIdsKey.nextIndexOf(id, idx);
            sumValues[idx] += left.entryValues[i];
        }
        val rightLength = leftIdsKey.length();
        idx = 0;
        for(int i = 0; i < rightLength; i++) {
            int id = rightIdsKey.at(i);
            idx = unionIdsKey.nextIndexOf(id, idx);
            sumValues[idx] += left.entryValues[i];
        }
        return new CompactFlameGraphValue(unionSetEntry.id, sumValues);
    }

    public CompactFlameGraphValue scale(double coef) {
        val len = entryValues.length;
        val resValues = new int[len];
        for(int i = 0; i < len; i++) {
            resValues[i] = (int) (entryValues[i] * coef);
        }
        return new CompactFlameGraphValue(entrySetId, resValues);
    }

    //---------------------------------------------------------------------------------------------

    public static SumScaleValueSupport<CompactFlameGraphValue> opsSupportFor(FlameGraphSignatureRegistry registry) {
        return new CompactFlameGraphSumScaleValueSupport(registry);
    }

    /**
     * implementation of SumScaleValueSupport<CompactFlameGraphValue> for a given registry
     */
    @RequiredArgsConstructor
    public static class CompactFlameGraphSumScaleValueSupport extends SumScaleValueSupport<CompactFlameGraphValue> {
        private final FlameGraphSignatureRegistry registry;

        @Override
        public CompactFlameGraphValue sum(CompactFlameGraphValue left, CompactFlameGraphValue right) {
            return sumOf(left, right, registry);
        }

        @Override
        public CompactFlameGraphValue scalar(CompactFlameGraphValue src, double coef) {
            return src.scale(coef);
        }
    }
}
