package fr.an.spark.plugin.flamegraph.shared.protocol;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor
public class ResolveStackTracesResponse implements Serializable {

    public List<ResolveStackTraceResponse> resolvedStackTraces;

    @NoArgsConstructor @AllArgsConstructor
    public static class ResolveStackTraceResponse implements Serializable {
        public int fromEntryId;
        // public StackTraceElementKey[] remainElements;
        public int[] remainElementIds;
    }

}
