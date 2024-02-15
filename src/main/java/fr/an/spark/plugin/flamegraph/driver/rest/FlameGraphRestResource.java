package fr.an.spark.plugin.flamegraph.driver.rest;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphPluginInfoDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import lombok.val;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 *
 */
@Path("/v1")
@Produces({ MediaType.APPLICATION_JSON })
public class FlameGraphRestResource extends AbstractFlameGraphRestResource {

    @GET
    @Path("info")
    public FlameGraphPluginInfoDTO info() {
        return new FlameGraphPluginInfoDTO("1.0");
    }

    @GET
    @Path("stackRegistryEntries")
    public List<StackTraceEntryDTO> stackRegistryEntries() {
        val plugin = flameGraphService();
        return plugin.listStackRegistryEntries();
    }

    @GET
    @Path("flameGraph")
    public FlameGraphNodeDTO flameGraph() {
        val plugin = flameGraphService();
        return plugin.currFlameGraphDTO();
    }

}
