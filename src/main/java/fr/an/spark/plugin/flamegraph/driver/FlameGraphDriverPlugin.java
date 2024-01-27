package fr.an.spark.plugin.flamegraph.driver;

import fr.an.spark.plugin.flamegraph.shared.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.spark.SparkContext;
import org.apache.spark.api.plugin.DriverPlugin;
import org.apache.spark.api.plugin.PluginContext;
import org.apache.spark.flamegraph.ui.FlameGraphUI;
import org.apache.spark.ui.SparkUI;

import javax.annotation.concurrent.GuardedBy;
import java.util.HashMap;
import java.util.Map;

/**
 * DriverPlugin implementation for FlameGraph
 * created from FlameGraphSparkPlugin
 */
@Slf4j
public class FlameGraphDriverPlugin implements DriverPlugin {

    private final Object lock = new Object();

    @GuardedBy("lock")
    private final StackTraceEntryRegistry stackTraceEntryRegistry = new StackTraceEntryRegistry();

    private int resolveStackTracesRequestCount;

    //---------------------------------------------------------------------------------------------

    public FlameGraphDriverPlugin() {
    }

    //---------------------------------------------------------------------------------------------

    @Override
    public Map<String, String> init(SparkContext sc, PluginContext pluginContext) {
        Map<String,String> executorInitValues = new HashMap<>();
        log.info("FlameGraph plugin init");

        SparkUI ui = sc.ui().getOrElse(() -> (SparkUI)null);
        new FlameGraphUI(this, ui);

        return executorInitValues;
    }

    @Override
    public void shutdown() {
        log.info("FlameGraph plugin shutdown");
    }

    @Override
    public void registerMetrics(String appId, PluginContext pluginContext) {
        // MetricRegistry metricRegistry = pluginContext.metricRegistry();
    }

    @Override
    public Object receive(Object message) throws Exception {
        Object res = null;

        if (message instanceof ResolveStackTracesRequest) {
            ResolveStackTracesRequest req = (ResolveStackTracesRequest) message;
            resolveStackTracesRequestCount++;
            int totalRemainElementCount = 0;
            for(val toResolve: req.toResolve) {
                totalRemainElementCount += toResolve.remainElements.length;
            }
            log.info("FlameGraph driver receive ResolveStackTracesRequest[" + resolveStackTracesRequestCount + "]: "
                    + "toResolveCount:" + req.toResolve.size()
                    + " total remainElementCount:" + totalRemainElementCount
            );
            ResolveStackTracesResponse resp;
            synchronized (lock) {
                resp = stackTraceEntryRegistry.handleResolveStackTracesRequest(req);
            }
            return resp;
        } else if (message instanceof SubmitFlameGraphCounterChangeRequest) {
            SubmitFlameGraphCounterChangeRequest req = (SubmitFlameGraphCounterChangeRequest) message;
            // do nothing
            SubmitFlameGraphCounterChangeResponse resp = new SubmitFlameGraphCounterChangeResponse();
            return resp;
        }

        return res;
    }

}
