package fr.an.spark.plugin.flamegraph.driver.rest.dto;

import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ThreadGroupsFlameGraphDTO implements Serializable {
    public List<ThreadGroupFlameGraphDTO> threadGroups = new ArrayList<>();

    public void add(String threadGroup, FlameGraphNodeDTO root) {
        threadGroups.add(new ThreadGroupFlameGraphDTO(threadGroup, root));
    }

    @AllArgsConstructor
    public static class ThreadGroupFlameGraphDTO implements Serializable {
        public String name;
        public FlameGraphNodeDTO root;
    }

}
