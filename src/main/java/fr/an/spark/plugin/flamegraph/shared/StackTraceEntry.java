package fr.an.spark.plugin.flamegraph.shared;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

// @RequiredArgsConstructor
public class StackTraceEntry {

    public final int id;
    public final StackTraceEntry parent;
    public final StackTraceElementKey stackTraceElementKey;

    private Map<StackTraceElementKey,StackTraceEntry> childMap = new HashMap<>();

    //---------------------------------------------------------------------------------------------

    public StackTraceEntry(int id, StackTraceEntry parent, StackTraceElementKey stackTraceElementKey) {
        this.id = id;
        this.parent = parent;
        this.stackTraceElementKey = stackTraceElementKey;
    }

    public StackTraceEntry findChild(StackTraceElementKey childKey) {
        return childMap.get(childKey);
    }

    /*pp*/ void _registerChild(StackTraceEntry child) {
        childMap.put(child.stackTraceElementKey, child);
    }

    public StackTraceEntryDTO toDTO() {
        return new StackTraceEntryDTO(id, (parent!=null)? parent.id : 0, stackTraceElementKey);
    }

}
