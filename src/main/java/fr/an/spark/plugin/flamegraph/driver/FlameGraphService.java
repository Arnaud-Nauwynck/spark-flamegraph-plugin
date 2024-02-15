package fr.an.spark.plugin.flamegraph.driver;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import fr.an.spark.plugin.flamegraph.shared.FlameGraphThreadGroupsChangeAccumulator;
import fr.an.spark.plugin.flamegraph.shared.TimeScaleSeries;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesResponse;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeResponse;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntryRegistry;
import fr.an.spark.plugin.flamegraph.shared.value.ImmutableCompactIdFlameGraphValue;
import fr.an.spark.plugin.flamegraph.shared.value.ImmutableCompactIdFlameGraphValue.ImmutableCompactIdFlameGraphSumScaleValueSupport;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;

/**
 * Singleton facade for storing FlameGraph(s) for spark driver and executors.
 */
@Slf4j
public class FlameGraphService {

    private boolean verbose = false;

    private final StackTraceEntryRegistry stackTraceEntryRegistry = new StackTraceEntryRegistry();

    private int resolveStackTracesRequestCount;

    private final FlameGraphSignatureRegistry flameGraphSignatureRegistry = new FlameGraphSignatureRegistry();

    protected FlameGraphThreadGroupsChangeAccumulator driverFlameGraphThreadGroupsChangeAccumulator = new FlameGraphThreadGroupsChangeAccumulator();

    protected FlameGraphThreadGroupsChangeAccumulator executorFlameGraphThreadGroupsChangeAccumulator = new FlameGraphThreadGroupsChangeAccumulator();

    protected final ImmutableCompactIdFlameGraphSumScaleValueSupport compactFlameGraphOps = new ImmutableCompactIdFlameGraphSumScaleValueSupport(flameGraphSignatureRegistry);

    protected final TimeScaleSeries<ImmutableCompactIdFlameGraphValue> driverFlameTimeSeries;
    protected final TimeScaleSeries<ImmutableCompactIdFlameGraphValue> executorFlameTimeSeries;

    //---------------------------------------------------------------------------------------------

    public FlameGraphService() {
        val scaleLengths = new int[] { 3, 3, 3, 3, 3, 3, 3, 3 }; // may be configurable
        this.driverFlameTimeSeries = new TimeScaleSeries<>(scaleLengths, compactFlameGraphOps);
        this.executorFlameTimeSeries = new TimeScaleSeries<>(scaleLengths, compactFlameGraphOps);;
    }

    //---------------------------------------------------------------------------------------------

    public ResolveStackTracesResponse handleResolveStackTraceRequest(ResolveStackTracesRequest req) {
        resolveStackTracesRequestCount++;
        int totalRemainElementCount = 0;
        for(val toResolve: req.toResolve) {
            totalRemainElementCount += toResolve.remainElements.length;
        }
        if (verbose) {
            log.info("FlameGraph driver receive ResolveStackTracesRequest[" + resolveStackTracesRequestCount + "]: "
                    + "toResolveCount:" + req.toResolve.size()
                    + " total remainElementCount:" + totalRemainElementCount
            );
        }
        ResolveStackTracesResponse resp = stackTraceEntryRegistry.handleResolveStackTracesRequest(req);
        return resp;
    }

    public List<StackTraceEntryDTO> listStackRegistryEntries() {
        return stackTraceEntryRegistry.listStackRegistryEntries();
    }


    public FlameGraphNodeDTO currFlameGraphDTO() {
        FlameGraphNodeDTO root = new FlameGraphNodeDTO("root", 100);

        FlameGraphNodeDTO f1 = new FlameGraphNodeDTO("f1", 40);
        root.addChild(f1);

        FlameGraphNodeDTO g1 = new FlameGraphNodeDTO("g1", 39);
        f1.addChild(g1);

        FlameGraphNodeDTO h1 = new FlameGraphNodeDTO("h1", 38);
        g1.addChild(h1);

        root.addChild(new FlameGraphNodeDTO("f2", 30));

        return root;
    }


    public SubmitFlameGraphCounterChangeResponse submitDriverFlameGraphCounterChange(SubmitFlameGraphCounterChangeRequest changeReq) {
        return null; // TODO
    }
}
