package fr.an.spark.plugin.flamegraph.shared.stacktrace;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTO;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.FlameGraphNodeDTOBuilder;
import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

// @RequiredArgsConstructor
public class StackTraceEntry implements Comparable<StackTraceEntry> {

    public final int id;
    public final StackTraceEntry parent;
    public final StackTraceElementKey stackTraceElementKey;

    public final int stackTraceLen;

    private Map<StackTraceElementKey,StackTraceEntry> childMap = new HashMap<>();

    //---------------------------------------------------------------------------------------------

    public StackTraceEntry(int id, StackTraceEntry parent, StackTraceElementKey stackTraceElementKey) {
        this.id = id;
        this.parent = parent;
        this.stackTraceLen = 1 + ((parent != null)? parent.stackTraceLen : 0);
        this.stackTraceElementKey = stackTraceElementKey;
    }

    //---------------------------------------------------------------------------------------------

    @Override
    public int compareTo(StackTraceEntry other) {
        return Integer.compare(id, other.id);
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

    public StackTraceEntry[] toStackTrace() {
        val len = stackTraceLen;
        val res = new StackTraceEntry[len];
        StackTraceEntry curr = this;
        for(int i = 0; i < len; i++, curr = curr.parent) {
            res[i] = curr;
        }
        return res;
    }

    public void addTo(FlameGraphNodeDTOBuilder builder, double addValue) {
        val stackTrace = toStackTrace();
        val len = stackTrace.length;
        FlameGraphNodeDTOBuilder currBuilder = builder;
        currBuilder.addValue(addValue);
        for(int i = 0; i < len; i++) {
            val eltKey = stackTrace[i].stackTraceElementKey;
            currBuilder = currBuilder.getOrCreateChild(eltKey);
            currBuilder.addValue(addValue);
        }
    }

}
