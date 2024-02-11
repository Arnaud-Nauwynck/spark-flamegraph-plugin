package fr.an.spark.plugin.flamegraph.driver.rest.dto;

import fr.an.spark.plugin.flamegraph.shared.StackTraceElementKey;
import fr.an.spark.plugin.flamegraph.shared.StackTraceEntry;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO class corresponding to {@link StackTraceEntry}
 */
@Data @AllArgsConstructor
public class StackTraceEntryDTO {
    public int id;
    public int parentId;
    public StackTraceElementKey element;
}
