package fr.an.spark.plugin.flamegraph.driver.rest;

import fr.an.spark.plugin.flamegraph.driver.FlameGraphService;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

public abstract class AbstractFlameGraphRestResource {

    @Context
    protected ServletContext servletContext;

    @Context
    protected HttpServletRequest httpRequest;

    protected FlameGraphService flameGraphService() {
        return FlameGraphServiceFromServletContext.get(servletContext);
    }

}
