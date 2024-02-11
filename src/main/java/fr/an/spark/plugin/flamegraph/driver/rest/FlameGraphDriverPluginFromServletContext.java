package fr.an.spark.plugin.flamegraph.driver.rest;

import fr.an.spark.plugin.flamegraph.driver.FlameGraphDriverPlugin;
import lombok.val;
import org.sparkproject.jetty.server.handler.ContextHandler;

import javax.servlet.ServletContext;

public class FlameGraphDriverPluginFromServletContext {

    private static final String attribute = FlameGraphDriverPluginFromServletContext.class.getCanonicalName();

    public static void set(ContextHandler contextHandler, FlameGraphDriverPlugin driverPlugin) {
        contextHandler.setAttribute(attribute, driverPlugin);
    }

    public static FlameGraphDriverPlugin get(ServletContext context) {
        return (FlameGraphDriverPlugin) context.getAttribute(attribute);
    }

}
