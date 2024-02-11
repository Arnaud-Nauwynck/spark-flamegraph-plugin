package fr.an.spark.plugin.flamegraph.driver.rest.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class FlameGraphNodeDTO {

    private String name;
    private int value;
    private List<FlameGraphNodeDTO> children = new ArrayList<FlameGraphNodeDTO>();

    public FlameGraphNodeDTO() {
    }

    public FlameGraphNodeDTO(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public void addChild(FlameGraphNodeDTO v) {
        this.children.add(v);
    }
}
