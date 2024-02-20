package fr.an.spark.plugin.flamegraph.driver;

import fr.an.spark.plugin.flamegraph.driver.rest.FlameGraphRestResource;
import fr.an.spark.plugin.flamegraph.driver.rest.FlameGraphServiceFromServletContext;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntry;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntryRegistry;
import fr.an.spark.plugin.flamegraph.shared.utils.ThreadNameUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.spark.SparkContext;
import org.apache.spark.api.plugin.DriverPlugin;
import org.apache.spark.api.plugin.PluginContext;
import org.apache.spark.flamegraph.ui.FlameGraphUI;
import org.apache.spark.ui.SparkUI;
import org.apache.spark.util.ThreadUtils$;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.sparkproject.jetty.servlet.DefaultServlet;
import org.sparkproject.jetty.servlet.ServletContextHandler;
import org.sparkproject.jetty.servlet.ServletHolder;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Spark DriverPlugin implementation for FlameGraph
 * created from FlameGraphSparkPlugin
 */
@Slf4j
public class FlameGraphDriverPlugin implements DriverPlugin {

    private static final String FLAMEGRAPH_PLUGIN_STATIC_RESOURCE_DIR = "fr/an/spark/plugin/flamegraph/ui/static";

    @Getter
    private final FlameGraphService flameGraphService = new FlameGraphService();

    protected ScheduledExecutorService scheduledExecutorService;
    protected int periodicThreadDumpMillis;
    protected ScheduledFuture<?> periodicScheduledFuture;
    protected long lastSnapshotMillis;

    protected final StackTraceEntryRegistry stackTraceEntryRegistry = new StackTraceEntryRegistry();

    //---------------------------------------------------------------------------------------------

    public FlameGraphDriverPlugin() {
    }

    // implement DriverPlugin
    //---------------------------------------------------------------------------------------------

    @Override
    public Map<String, String> init(SparkContext sc, PluginContext pluginContext) {
        Map<String, String> executorInitValues = new HashMap<>();
        log.info("FlameGraph plugin init");


        SparkUI ui = sc.ui().getOrElse(null);
        if (ui != null) {
            new FlameGraphUI(flameGraphService, ui);

            val cl = FlameGraphDriverPlugin.class.getClassLoader();

            // attach Rest handler for "/flamegraph-plugin/api/*"
            ui.attachHandler(createServletContextHandler(cl, "/flamegraph-plugin/api"));

            // attach static resources handler for "/flamegraph-plugin/static/*"
            ui.attachHandler(createStaticHandler(cl, FLAMEGRAPH_PLUGIN_STATIC_RESOURCE_DIR, "/flamegraph-plugin/static"));
        }

        if (! sc.isLocal()) {
            this.scheduledExecutorService = ThreadUtils$.MODULE$.newDaemonSingleThreadScheduledExecutor("flamegraph-poller");
            this.periodicThreadDumpMillis = sc.conf().getInt("spark.flamegraph.periodicThreadDumpMillis", 10_000);
            periodicThreadDumpMillis = Math.max(250, periodicThreadDumpMillis);
            this.lastSnapshotMillis = System.currentTimeMillis();
            this.periodicScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                    this::onPeriodicTakeThreadDump, periodicThreadDumpMillis, periodicThreadDumpMillis, TimeUnit.MILLISECONDS);
        }

        this.flameGraphService.start(sc);
        return executorInitValues;
    }

    protected ServletContextHandler createServletContextHandler(ClassLoader cl, String path) {
        val servletContextHolder = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHolder.setContextPath(path);
        servletContextHolder.setClassLoader(cl);
        FlameGraphServiceFromServletContext.set(servletContextHolder, flameGraphService);

        val holder = new ServletHolder(ServletContainer.class);
        holder.setInitParameter(ServerProperties.PROVIDER_PACKAGES,
                FlameGraphRestResource.class //
                        .getName().replace(".FlameGraphRestResource", "") // idem .getPackageName(), in java 8
        );
        servletContextHolder.addServlet(holder, "/*");
        return servletContextHolder;
    }

    protected static ServletContextHandler createStaticHandler(ClassLoader cl, String resourceBase, String path) {
        val contextHandler = new ServletContextHandler();
        contextHandler.setInitParameter("org.eclipse.jetty.servlet.Default.gzip", "false");
        val staticHandler = new DefaultServlet();
        val holder = new ServletHolder(staticHandler);

        URL resourceBaseURL = cl.getResource(resourceBase);
        if (resourceBaseURL != null) {
            holder.setInitParameter("resourceBase", resourceBaseURL.toString());
        } else {
            throw new RuntimeException("Could not find resource path for Web UI: " + resourceBase);
        }
        contextHandler.setContextPath(path);
        contextHandler.addServlet(holder, "/");
        return contextHandler;
    }

    @Override
    public void shutdown() {
        log.info("FlameGraph plugin shutdown");
        this.flameGraphService.stop();
    }

    @Override
    public void registerMetrics(String appId, PluginContext pluginContext) {
        // MetricRegistry metricRegistry = pluginContext.metricRegistry();
    }

    @Override
    public Object receive(Object message) throws Exception {
        if (message instanceof ResolveStackTracesRequest) {
            val req = (ResolveStackTracesRequest) message;
            val resp = flameGraphService.handleResolveStackTraceRequest(req);
            return resp;
        } else if (message instanceof SubmitFlameGraphCounterChangeRequest) {
            val req = (SubmitFlameGraphCounterChangeRequest) message;
            val resp = flameGraphService.applyExecutorFlameGraphCounterChange(req);
            return resp;
        } else {
            // unrecognized request ?
            return null;
        }
    }

    //---------------------------------------------------------------------------------------------

    public List<StackTraceEntryDTO> listStackRegistryEntries() {
        return flameGraphService.listStackRegistryEntries();
    }

    public FlameGraphNodeDTO currFlameGraphDTO() {
        return flameGraphService.currFlameGraphDTO();
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

        long now = System.currentTimeMillis();
        int elapsedMillis = (int)(now - lastSnapshotMillis);
        this.lastSnapshotMillis = now;

        for(int i = 0; i < threadCount; i++) {
            val threadInfo = threadInfos[i];
            val stackTraceEntries = stackTraceEntryRegistry.localResolveOrRegisterStackTrace(threadInfo);

            if (stackTraceEntries == null || stackTraceEntries.length == 0 || stackTraceEntries[0] == null) {
                log.error("should not occur ..ignore");
                continue;
            }
            StackTraceEntry resolvedEntry = stackTraceEntries[0];
            String threadName = threadInfo.getThreadName();
            String threadGroupName = ThreadNameUtils.templatizeThreadName(threadName);

            flameGraphService.applyDriverFlameGraphCounterAdd(threadGroupName, resolvedEntry, elapsedMillis);
        }
    }

}
