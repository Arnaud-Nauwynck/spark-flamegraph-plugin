package fr.an.spark.plugin.flamegraph.driver.rest;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphPluginInfoDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import lombok.val;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

/**
 *
 */
@Path("/v1")
public class FlameGraphRestResource extends AbstractFlameGraphRestResource {

    @GET
    @Path("info")
    public FlameGraphPluginInfoDTO info() {
        return new FlameGraphPluginInfoDTO("1.0");
    }

    @GET
    @Path("stackRegistryEntries")
    public List<StackTraceEntryDTO> stackRegistryEntries() {
        val plugin = flameGraphDriverPlugin();
        return plugin.listStackRegistryEntries();
    }

}