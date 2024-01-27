package fr.an.spark.plugin.flamegraph;

import fr.an.spark.plugin.flamegraph.driver.FlameGraphDriverPlugin;
import fr.an.spark.plugin.flamegraph.executor.FlameGraphExecutorPlugin;
import org.apache.spark.api.plugin.DriverPlugin;
import org.apache.spark.api.plugin.ExecutorPlugin;
import org.apache.spark.api.plugin.SparkPlugin;

/**
 * created by introspection, from spark plugins configuration
 */
public class FlameGraphSparkPlugin implements SparkPlugin {

    public FlameGraphSparkPlugin() {
    }

    @Override
    public DriverPlugin driverPlugin() {
        return new FlameGraphDriverPlugin();
    }

    @Override
    public ExecutorPlugin executorPlugin() {
        return new FlameGraphExecutorPlugin();
    }
}