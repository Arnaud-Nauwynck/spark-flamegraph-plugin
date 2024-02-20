package fr.an.spark.plugin.flamegraph.driver.rest.dto;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
public class TimeRangeSparkFlameGraphsDTO implements Serializable {

    public long fromTime;
    public long toTime;
    public ThreadGroupsFlameGraphDTO driver;
    public ThreadGroupsFlameGraphDTO executors;

}
