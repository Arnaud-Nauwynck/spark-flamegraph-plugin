package fr.an.spark.plugin.flamegraph.driver;

import fr.an.spark.plugin.flamegraph.shared.FlameGraphThreadGroupsChangeAccumulator;

public class FlameGraphService {

    protected FlameGraphThreadGroupsChangeAccumulator driverFlameGraphThreadGroupsChangeAccumulator = new FlameGraphThreadGroupsChangeAccumulator();

    protected FlameGraphThreadGroupsChangeAccumulator executorFlameGraphThreadGroupsChangeAccumulator = new FlameGraphThreadGroupsChangeAccumulator();

}
