package fr.an.spark.plugin.flamegraph.shared.value;

import com.google.common.collect.ImmutableMap;
import fr.an.spark.plugin.flamegraph.shared.SumScaleValueSupport;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import fr.an.spark.plugin.flamegraph.shared.value.CompactFlameGraphValue.CompactFlameGraphSumScaleValueSupport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable Map<String threadGroup, CompactFlameGraphValue>
 */
public class ThreadGroupsCompactFlameGraphValue {

    @Getter
    private final ImmutableMap<String, CompactFlameGraphValue> perThreadGroups;

    //---------------------------------------------------------------------------------------------

    public ThreadGroupsCompactFlameGraphValue(Map<String, CompactFlameGraphValue> perThreadGroups) {
        this.perThreadGroups = ImmutableMap.copyOf(perThreadGroups);
    }

    public static ThreadGroupsCompactFlameGraphValue sumOf(ThreadGroupsCompactFlameGraphValue left,
                                                           ThreadGroupsCompactFlameGraphValue right,
                                                           FlameGraphSignatureRegistry registry) {
        val res = new HashMap<String, CompactFlameGraphValue>();
        val remainRight = new HashMap<>(right.perThreadGroups);
        for(val e: left.perThreadGroups.entrySet()) {
            val key = e.getKey();
            CompactFlameGraphValue leftElt = e.getValue();
            CompactFlameGraphValue rightEltOrNull = remainRight.remove(key);
            val resElt = (rightEltOrNull != null)? CompactFlameGraphValue.sumOf(leftElt, rightEltOrNull, registry)
                    : leftElt;
            res.put(key, resElt);
        }
        res.putAll(remainRight);
        return new ThreadGroupsCompactFlameGraphValue(res);
    }

    public ThreadGroupsCompactFlameGraphValue scale(double coef) {
        val res = new HashMap<String, CompactFlameGraphValue>();
        for(val e: perThreadGroups.entrySet()) {
            CompactFlameGraphValue elt = e.getValue();
            val resElt = elt.scale(coef);
            res.put(e.getKey(), resElt);
        }
        return new ThreadGroupsCompactFlameGraphValue(res);
    }

    //---------------------------------------------------------------------------------------------

    public static SumScaleValueSupport<ThreadGroupsCompactFlameGraphValue> opsSupportFor(FlameGraphSignatureRegistry registry) {
        return new ThreadGroupsCompactFlameGraphSumScaleValueSupport(registry);
    }

    /**
     * implementation of SumScaleValueSupport<ThreadGroupsCompactFlameGraphValue> for a given registry
     */
    @RequiredArgsConstructor
    public static class ThreadGroupsCompactFlameGraphSumScaleValueSupport extends SumScaleValueSupport<ThreadGroupsCompactFlameGraphValue> {
        private final FlameGraphSignatureRegistry registry;
        private final SumScaleValueSupport<CompactFlameGraphValue> valueOps;

        public ThreadGroupsCompactFlameGraphSumScaleValueSupport(FlameGraphSignatureRegistry registry) {
            this.registry = registry;
            this.valueOps = new CompactFlameGraphSumScaleValueSupport(registry);
        }

        @Override
        public ThreadGroupsCompactFlameGraphValue sum(ThreadGroupsCompactFlameGraphValue left, ThreadGroupsCompactFlameGraphValue right) {
            return sumOf(left, right, registry);
        }

        @Override
        public ThreadGroupsCompactFlameGraphValue scalar(ThreadGroupsCompactFlameGraphValue src, double coef) {
            return src.scale(coef);
        }
    }

}
