package fr.an.spark.plugin.flamegraph.driver;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.ThreadGroupsFlameGraphDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.TimeRangeSparkFlameGraphsDTO;
import fr.an.spark.plugin.flamegraph.shared.FlameGraphThreadGroupsChangeAccumulator;
import fr.an.spark.plugin.flamegraph.shared.ThreadGroupsFlameGraphTimeScaleSeries;
import fr.an.spark.plugin.flamegraph.shared.TimeScaleSeries.AccTimeRange;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesResponse;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeResponse;
import fr.an.spark.plugin.flamegraph.shared.signature.FlameGraphSignatureRegistry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntryRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.spark.SparkContext;
import org.apache.spark.util.ThreadUtils$;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Singleton facade for storing FlameGraph(s) for spark driver and executors.
 */
@Slf4j
public class FlameGraphService {

    private boolean verbose = false;

    private final StackTraceEntryRegistry stackTraceEntryRegistry = new StackTraceEntryRegistry();

    private int resolveStackTracesRequestCount;

    private final FlameGraphSignatureRegistry flameGraphSignatureRegistry = new FlameGraphSignatureRegistry(stackTraceEntryRegistry);

    protected FlameGraphThreadGroupsChangeAccumulator driverFlameGraphThreadGroupsChangeAccumulator = new FlameGraphThreadGroupsChangeAccumulator();

    protected FlameGraphThreadGroupsChangeAccumulator executorFlameGraphThreadGroupsChangeAccumulator = new FlameGraphThreadGroupsChangeAccumulator();


    protected ScheduledExecutorService scheduledExecutorService;
    protected int periodicAddToTimeSeriesSeconds;
    protected ScheduledFuture<?> periodicScheduledFuture;
    protected long lastAddMillis;

    protected final ThreadGroupsFlameGraphTimeScaleSeries driverFlameTimeSeries;
    protected final ThreadGroupsFlameGraphTimeScaleSeries executorFlameTimeSeries;

    //---------------------------------------------------------------------------------------------

    public FlameGraphService() {
        val scaleLengths = ThreadGroupsFlameGraphTimeScaleSeries.defaultScaleLengths; // may be configurable
        this.driverFlameTimeSeries = new ThreadGroupsFlameGraphTimeScaleSeries(scaleLengths, flameGraphSignatureRegistry);
        this.executorFlameTimeSeries = new ThreadGroupsFlameGraphTimeScaleSeries(scaleLengths, flameGraphSignatureRegistry);

        this.scheduledExecutorService = ThreadUtils$.MODULE$.newDaemonSingleThreadScheduledExecutor("flamegraph-timeseries-acc");
    }

    public void start(SparkContext sc) {
        this.periodicAddToTimeSeriesSeconds = Math.max(10, sc.conf().getInt("spark.flamegraph.periodicAddToTimeSeriesSeconds", 30));
        this.lastAddMillis = System.currentTimeMillis();
        this.periodicScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::onPeriodicAddToTimeSeries, periodicAddToTimeSeriesSeconds, periodicAddToTimeSeriesSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        val f = periodicScheduledFuture;
        if (f != null) {
            this.periodicScheduledFuture = null;
            f.cancel(false);
        }
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

    public void applyDriverFlameGraphCounterAdd(
            String threadGroupName, StackTraceEntry resolvedEntry, int elapsedMillis
            ) {
        driverFlameGraphThreadGroupsChangeAccumulator.addToThreadGroupStackEntry(
                threadGroupName, resolvedEntry, elapsedMillis);
    }

    public SubmitFlameGraphCounterChangeResponse applyExecutorFlameGraphCounterChange(SubmitFlameGraphCounterChangeRequest changeReq) {
        return executorFlameGraphThreadGroupsChangeAccumulator.applyChangeRequest(changeReq);
    }

    protected void onPeriodicAddToTimeSeries() {
        val startTime = lastAddMillis;
        val endTime = System.currentTimeMillis();
        this.lastAddMillis = endTime;

        val driverFlameValue = driverFlameGraphThreadGroupsChangeAccumulator.resetAndGetThreadGroupsCompactValue(flameGraphSignatureRegistry);
        val driverTimeRange = new AccTimeRange<>(startTime, endTime, driverFlameValue);
        this.driverFlameTimeSeries.shiftAdd(driverTimeRange);

        val executorFlameValue = executorFlameGraphThreadGroupsChangeAccumulator.resetAndGetThreadGroupsCompactValue(flameGraphSignatureRegistry);
        val executorTimeRange = new AccTimeRange<>(startTime, endTime, executorFlameValue);
        this.executorFlameTimeSeries.shiftAdd(executorTimeRange);
    }

    public TimeRangeSparkFlameGraphsDTO extractTimeRangeSparkFlameGraphsDTO(long fromTime, long toTime) {
        ThreadGroupsFlameGraphDTO driver = driverFlameTimeSeries.extractTimeRangeFlameGraphsDTO(fromTime, toTime);
        ThreadGroupsFlameGraphDTO executors = executorFlameTimeSeries.extractTimeRangeFlameGraphsDTO(fromTime, toTime);
        return new TimeRangeSparkFlameGraphsDTO(fromTime, toTime, driver, executors);
    }
}
