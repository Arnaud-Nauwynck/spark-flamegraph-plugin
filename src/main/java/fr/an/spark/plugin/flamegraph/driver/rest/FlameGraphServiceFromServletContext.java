package fr.an.spark.plugin.flamegraph.driver.rest;

import fr.an.spark.plugin.flamegraph.driver.FlameGraphService;
import org.sparkproject.jetty.server.handler.ContextHandler;

import javax.servlet.ServletContext;

public class FlameGraphServiceFromServletContext {

    private static final String attribute = FlameGraphServiceFromServletContext.class.getCanonicalName();

    public static void set(ContextHandler contextHandler, FlameGraphService p) {
        contextHandler.setAttribute(attribute, p);
    }

    public static FlameGraphService get(ServletContext context) {
        return (FlameGraphService) context.getAttribute(attribute);
    }

}
