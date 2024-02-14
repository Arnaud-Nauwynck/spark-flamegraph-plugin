package fr.an.spark.plugin.flamegraph.driver;

import fr.an.spark.plugin.flamegraph.driver.rest.FlameGraphDriverPluginFromServletContext;
import fr.an.spark.plugin.flamegraph.driver.rest.FlameGraphRestResource;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesResponse;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.SubmitFlameGraphCounterChangeResponse;
import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceEntryRegistry;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.spark.SparkContext;
import org.apache.spark.api.plugin.DriverPlugin;
import org.apache.spark.api.plugin.PluginContext;
import org.apache.spark.flamegraph.ui.FlameGraphUI;
import org.apache.spark.ui.SparkUI;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.sparkproject.jetty.servlet.DefaultServlet;
import org.sparkproject.jetty.servlet.ServletContextHandler;
import org.sparkproject.jetty.servlet.ServletHolder;

import javax.annotation.concurrent.GuardedBy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DriverPlugin implementation for FlameGraph
 * created from FlameGraphSparkPlugin
 */
@Slf4j
public class FlameGraphDriverPlugin implements DriverPlugin {

    private static final String FLAMEGRAPH_PLUGIN_STATIC_RESOURCE_DIR = "fr/an/spark/plugin/flamegraph/ui/static";

    private final Object lock = new Object();

    private boolean verbose = false;

    @GuardedBy("lock")
    private final StackTraceEntryRegistry stackTraceEntryRegistry = new StackTraceEntryRegistry();

    private int resolveStackTracesRequestCount;

    //---------------------------------------------------------------------------------------------

    public FlameGraphDriverPlugin() {
    }

    // implement DriverPlugin
    //---------------------------------------------------------------------------------------------

    @Override
    public Map<String, String> init(SparkContext sc, PluginContext pluginContext) {
        Map<String,String> executorInitValues = new HashMap<>();
        log.info("FlameGraph plugin init");

        SparkUI ui = sc.ui().getOrElse(null);
        if (ui != null) {
            new FlameGraphUI(this, ui);

            val cl = FlameGraphDriverPlugin.class.getClassLoader();

            // attach Rest handler for "/flamegraph-plugin/api/*"
            ui.attachHandler(createServletContextHandler(cl, "/flamegraph-plugin/api"));

            // attach static resources handler for "/flamegraph-plugin/static/*"
            ui.attachHandler(createStaticHandler(cl, FLAMEGRAPH_PLUGIN_STATIC_RESOURCE_DIR, "/flamegraph-plugin/static"));
        }

        return executorInitValues;
    }

    protected ServletContextHandler createServletContextHandler(ClassLoader cl, String path) {
        val servletContextHolder = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHolder.setContextPath(path);
        servletContextHolder.setClassLoader(cl);
        FlameGraphDriverPluginFromServletContext.set(servletContextHolder, this);

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
            if (verbose) {
                log.info("FlameGraph driver receive ResolveStackTracesRequest[" + resolveStackTracesRequestCount + "]: "
                        + "toResolveCount:" + req.toResolve.size()
                        + " total remainElementCount:" + totalRemainElementCount
                );
            }
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

    //---------------------------------------------------------------------------------------------

    public List<StackTraceEntryDTO> listStackRegistryEntries() {
        synchronized (lock) {
            return stackTraceEntryRegistry.listStackRegistryEntries();
        }
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

}
