package fr.an.spark.plugin.flamegraph.shared;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTOBuilder;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.ThreadGroupsFlameGraphDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.ThreadGroupsFlameGraphDTO.ThreadGroupFlameGraphDTO;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import fr.an.spark.plugin.flamegraph.shared.value.CompactFlameGraphValue;
import fr.an.spark.plugin.flamegraph.shared.value.ThreadGroupsCompactFlameGraphValue;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

public class ThreadGroupsFlameGraphTimeScaleSeries extends TimeScaleSeries<ThreadGroupsCompactFlameGraphValue> {

    public static final int[] defaultScaleLengths = new int[] { 3, 3, 3, 3, 3, 3, 3, 3 };

    protected final FlameGraphSignatureRegistry signatureRegistry;

    public ThreadGroupsFlameGraphTimeScaleSeries(int[] scaleLengths,
                                                 FlameGraphSignatureRegistry signatureRegistry) {
        super(scaleLengths, ThreadGroupsCompactFlameGraphValue.opsSupportFor(signatureRegistry));
        this.signatureRegistry = signatureRegistry;
    }

    public ThreadGroupsFlameGraphDTO extractTimeRangeFlameGraphsDTO(long fromTime, long toTime,
                                                                    ) {
        Map<String, FlameGraphNodeDTOBuilder> tmpres = super.extractTimeRange(fromTime, toTime,
                new ThreadGroupsFlameGraphDTOAccSupport(signatureRegistry));
        val res = new ThreadGroupsFlameGraphDTO();
        for(val e : tmpres.entrySet()) {
            res.add(e.getKey(), e.getValue());
        }
        return res;
    }

    @RequiredArgsConstructor
    protected static class ThreadGroupsFlameGraphDTOAccSupport
            extends SumScaleValueAccSupport<ThreadGroupsCompactFlameGraphValue, Map<String, FlameGraphNodeDTOBuilder>> {

        protected final FlameGraphSignatureRegistry signatureRegistry;

        @Override
        public Map<String, FlameGraphNodeDTOBuilder> createAcc() {
            return new HashMap<>();
        }

        @Override
        public void addTo(Map<String, FlameGraphNodeDTOBuilder> dest,
                          ThreadGroupsCompactFlameGraphValue src, double coef) {
            for(val e : src.getPerThreadGroups().entrySet()) {
                String threadGroup = e.getKey();
                CompactFlameGraphValue value = e.getValue();
                val dtoBuilder = dest.computeIfAbsent(threadGroup, k -> new FlameGraphNodeDTOBuilder(null));
                value.addTo(dtoBuilder, coef, signatureRegistry);
            }
        }
    }

}
