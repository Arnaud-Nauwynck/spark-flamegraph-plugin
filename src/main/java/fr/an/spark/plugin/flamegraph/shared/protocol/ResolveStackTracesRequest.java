package fr.an.spark.plugin.flamegraph.shared.protocol;

import fr.an.spark.plugin.flamegraph.shared.stacktrace.StackTraceElementKey;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ResolveStackTracesRequest implements Serializable {

    public List<ResolveStackTraceRequest> toResolve = new ArrayList<>();

    @AllArgsConstructor
    public static class ResolveStackTraceRequest implements Serializable {
        public int fromEntryId;
        public StackTraceElementKey[] remainElements;
    }

}
