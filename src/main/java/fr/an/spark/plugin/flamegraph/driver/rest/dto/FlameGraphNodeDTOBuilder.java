package fr.an.spark.plugin.flamegraph.driver.rest.dto;

import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceElementKey;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class FlameGraphNodeDTOBuilder {

    private final StackTraceElementKey key; // redundant with children Map key
    private double value;
    private Map<StackTraceElementKey,FlameGraphNodeDTOBuilder> children = new HashMap<>();

    public FlameGraphNodeDTOBuilder getOrCreateChild(StackTraceElementKey childKey) {
        return children.computeIfAbsent(childKey, k -> new FlameGraphNodeDTOBuilder(k));
    }

    public void addValue(double addValue) {
        this.value += addValue;
    }

    public FlameGraphNodeDTO build() {
        val res = new FlameGraphNodeDTO();
        if (key != null) {
            res.setName(key.toName());
        }
        res.setValue((int) value);
        if (children != null && !children.isEmpty()) {
            for(val e : children.entrySet()) {
                val childKey = e.getKey();
                val childBuilder = e.getValue();
                val child = childBuilder.build();
                res.addChild(child);
            }
        }
        return res;
    }

}
