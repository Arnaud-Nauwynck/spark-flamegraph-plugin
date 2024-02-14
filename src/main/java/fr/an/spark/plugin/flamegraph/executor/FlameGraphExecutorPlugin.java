package fr.an.spark.plugin.flamegraph.executor;

import fr.an.spark.plugin.flamegraph.shared.*;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest.ResolveStackTraceRequest;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntryRegistry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntryRegistry.PartiallyResolvedStackTrace;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesResponse;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeResponse;
import fr.an.spark.plugin.flamegraph.shared.utils.ThreadNameUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.spark.TaskContext;
import org.apache.spark.TaskFailedReason;
import org.apache.spark.api.plugin.ExecutorPlugin;
import org.apache.spark.api.plugin.PluginContext;
import org.apache.spark.util.ThreadUtils$;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * ExecutorPlugin implementation for FlameGraph
 * created from FlameGraphSparkPlugin
 */
@Slf4j
public class FlameGraphExecutorPlugin implements ExecutorPlugin {

    protected PluginContext pluginContext;

    protected ScheduledExecutorService scheduledExecutorService;
    protected int periodicThreadDumpMillis;
    protected ScheduledFuture<?> periodicScheduledFuture;

    protected final StackTraceEntryRegistry stackTraceEntryRegistry = new StackTraceEntryRegistry();

    @Getter
    protected FlameGraphThreadGroupsChangeAccumulator flameGraphThreadGroupsChangeAccumulator = new FlameGraphThreadGroupsChangeAccumulator();
    protected long lastSnapshotMillis;

    protected int submitFrequency = 10;
    protected int submitIndexModulo = submitFrequency;


    //---------------------------------------------------------------------------------------------

    public FlameGraphExecutorPlugin() {
    }

    // implements ExecutorPlugin
    //---------------------------------------------------------------------------------------------

    @Override
    public void init(PluginContext ctx, Map<String, String> extraConf) {
        this.pluginContext = ctx;
        log.info("FlameGraph plugin - init");
        this.scheduledExecutorService = ThreadUtils$.MODULE$.newDaemonSingleThreadScheduledExecutor("flamegraph-poller");
        this.periodicThreadDumpMillis = ctx.conf().getInt("spark.flamegraph.periodicThreadDumpMillis", 10_000);
        periodicThreadDumpMillis = Math.max(250, periodicThreadDumpMillis);
        this.lastSnapshotMillis = System.currentTimeMillis();
        this.periodicScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::onPeriodicTakeThreadDump, periodicThreadDumpMillis, periodicThreadDumpMillis, TimeUnit.MILLISECONDS);

    }

    @Override
    public void shutdown() {
        log.info("FlameGraph plugin - shutdown");
        if (periodicScheduledFuture != null) {
            periodicScheduledFuture.cancel(false);
            this.periodicScheduledFuture = null;
        }
        scheduledExecutorService.shutdownNow();
        this.scheduledExecutorService = null;
        this.pluginContext = null;
    }

    @Override
    public void onTaskStart() {
        TaskContext taskContext = TaskContext.get();
        log.info("FlameGraph plugin - onTaskStart partitionId:" + taskContext.partitionId()
                + " stageId:" + taskContext.stageId());
    }

    @Override
    public void onTaskSucceeded() {
        TaskContext taskContext = TaskContext.get();
        log.info("FlameGraph plugin - onTaskSucceeded partitionId:" + taskContext.partitionId()
                + " stageId:" + taskContext.stageId());
    }

    @Override
    public void onTaskFailed(TaskFailedReason failureReason) {
        TaskContext taskContext = TaskContext.get();
        log.info("FlameGraph plugin - onTaskFailed partitionId:" + taskContext.partitionId()
                + " stageId:" + taskContext.stageId());
    }

    //---------------------------------------------------------------------------------------------

    private void onPeriodicTakeThreadDump() {
        try {
            safeOnPeriodicTakeThreadDump();
        } catch(Exception ex) {
            log.info("ignore ex in periodic timer: " + ex.getMessage());
        }
    }

    private synchronized void safeOnPeriodicTakeThreadDump() {
        ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
        val threadCount = threadInfos.length;

        TmpResolvedThreadInfo[] resolvedThreadInfos = new TmpResolvedThreadInfo[threadCount];
        val resolveRequest = new ResolveStackTracesRequest();
        for(int i = 0; i < threadCount; i++) {
            val threadInfo = threadInfos[i];
            val partiallyResolved = stackTraceEntryRegistry.partialResolveStackTrace(threadInfo);
            resolvedThreadInfos[i] = new TmpResolvedThreadInfo(threadInfo, partiallyResolved);
            if (partiallyResolved.remainElementKeys != null) {
                resolveRequest.toResolve.add(new ResolveStackTraceRequest(partiallyResolved.resolvedEntry.id,
                        partiallyResolved.remainElementKeys));
            }
        }

        if (! resolveRequest.toResolve.isEmpty()) {
            // call executor -> driver to resolve all remaining StackTrace[] to entries
            ResolveStackTracesResponse resp;
            try {
                Object respObject = pluginContext.ask(resolveRequest);
                resp = (ResolveStackTracesResponse) respObject;
            } catch(Exception ex) {
                log.warn("Failed to call driver to resolve stackTrace entries! ... ignore, return");
                return;
            }

            // got response => add to local registry, then resume resolve StackTrace[] to entries
            stackTraceEntryRegistry.registerResolveResponse(resolveRequest, resp);

            // re-resolve (maybe remaining only)
            for(int i = 0; i < threadCount; i++) {
                val threadInfo = threadInfos[i];
                TmpResolvedThreadInfo resolvedThreadInfo = resolvedThreadInfos[i];
                if (resolvedThreadInfo.partiallyResolvedStackTrace.remainElementKeys != null) {
                    val partiallyResolved = stackTraceEntryRegistry.redoResolveStackTrace(resolvedThreadInfo.partiallyResolvedStackTrace, threadInfo);
                    if (partiallyResolved.remainElementKeys != null) {
                        log.error("should not occur.. StackTrace not resolved after request-response for resolution");
                        return;
                    }
                    resolvedThreadInfos[i] = new TmpResolvedThreadInfo(threadInfo, partiallyResolved);
                }
            }
        }

        // process (fully) resolved StackTraces, increment counters
        long now = System.currentTimeMillis();
        long elapsedMillis = now - lastSnapshotMillis;
        this.lastSnapshotMillis = now;

        for(int i = 0; i < threadCount; i++) {
            val threadInfo = threadInfos[i];
            StackTraceEntry resolvedEntry = resolvedThreadInfos[i].partiallyResolvedStackTrace.resolvedEntry;
            if (resolvedEntry == null) {
                log.error("should not occur ..ignore");
                continue;
            }
            String threadName = threadInfo.getThreadName();
            String threadGroupName = ThreadNameUtils.templatizeThreadName(threadName);

            flameGraphThreadGroupsChangeAccumulator.addToThreadGroupStackEntry(
                    threadGroupName, resolvedEntry, elapsedMillis);
        }

        // periodically send accumulated counters
        submitIndexModulo--;
        if (submitIndexModulo <= 0) {
            submitIndexModulo = submitFrequency;

            String executorId = pluginContext.executorID();

            long submitChangeTime = System.currentTimeMillis();
            // scan all modified entries since last time
            SubmitFlameGraphCounterChangeRequest submitChangeReq =
                    flameGraphThreadGroupsChangeAccumulator.createChangeRequest(executorId);

            // call executor -> driver to resolve all remaining StackTrace[] to entries
            SubmitFlameGraphCounterChangeResponse submitChangeResp;
            try {
                Object respObject = pluginContext.ask(submitChangeReq);
                submitChangeResp = (SubmitFlameGraphCounterChangeResponse) respObject;

                // update last modified time
                flameGraphThreadGroupsChangeAccumulator.onResponseUpdateLastTime(
                        submitChangeReq, submitChangeResp, submitChangeTime);
            } catch (Exception ex) {
                log.warn("Failed to call driver to submit flamegraph counter! ... ignore, return");
            }
        }
    }

    @RequiredArgsConstructor
    protected static class TmpResolvedThreadInfo {
        private final ThreadInfo threadInfo; // unused
        private final PartiallyResolvedStackTrace partiallyResolvedStackTrace;

    }

}
