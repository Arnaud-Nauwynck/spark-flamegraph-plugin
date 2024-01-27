package fr.an.spark.plugin.flamegraph.shared;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

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
