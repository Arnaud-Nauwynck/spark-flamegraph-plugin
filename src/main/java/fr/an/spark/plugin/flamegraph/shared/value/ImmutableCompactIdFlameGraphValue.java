package fr.an.spark.plugin.flamegraph.shared.value;

import fr.an.spark.plugin.flamegraph.shared.SumScaleValueSupport;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.Serializable;

public class ImmutableCompactIdFlameGraphValue implements Serializable {

    // ... implicit StackTraceIdSetRegistry stackTraceRegistry;
    private final int entrySetId;
    private final int[] entryValues;

    //---------------------------------------------------------------------------------------------

    public ImmutableCompactIdFlameGraphValue(int entrySetId,
                                             int[] entryValues // assume immutable, transfer ownership
    ) {
        this.entrySetId = entrySetId;
        this.entryValues = entryValues;
    }

    //---------------------------------------------------------------------------------------------

    public ImmutableCompactIdFlameGraphValue sum(ImmutableCompactIdFlameGraphValue other, FlameGraphSignatureRegistry registry) {
        return sumOf(this, other, registry);
    }

    public static ImmutableCompactIdFlameGraphValue sumOf(ImmutableCompactIdFlameGraphValue left, ImmutableCompactIdFlameGraphValue right, FlameGraphSignatureRegistry registry) {
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
        return new ImmutableCompactIdFlameGraphValue(unionSetEntry.id, sumValues);
    }

    public ImmutableCompactIdFlameGraphValue scale(double coef) {
        val len = entryValues.length;
        val resValues = new int[len];
        for(int i = 0; i < len; i++) {
            resValues[i] = (int) (entryValues[i] * coef);
        }
        return new ImmutableCompactIdFlameGraphValue(entrySetId, resValues);
    }

    /**
     * implementation of SumScaleValueSupport<ImmutableCompactIdFlameGraphValue> for a given registry
     */
    @RequiredArgsConstructor
    public static class ImmutableCompactIdFlameGraphSumScaleValueSupport extends SumScaleValueSupport<ImmutableCompactIdFlameGraphValue> {
        private final FlameGraphSignatureRegistry registry;

        @Override
        public ImmutableCompactIdFlameGraphValue sum(ImmutableCompactIdFlameGraphValue left, ImmutableCompactIdFlameGraphValue right) {
            return sumOf(left, right, registry);
        }

        @Override
        public ImmutableCompactIdFlameGraphValue scalar(ImmutableCompactIdFlameGraphValue src, double coef) {
            return src.scale(coef);
        }
    }
}
