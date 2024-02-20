package fr.an.spark.plugin.flamegraph.shared;

public abstract class SumScaleValueAccSupport<T, TAcc> {

    public abstract TAcc createAcc();
    public void addTo(TAcc dest, T src) {
        addTo(dest, src, 1.0);
    }
    public abstract void addTo(TAcc dest, T src, double coef);

    public void addTimeRangeIntersectTo(TAcc dest,
                                        long fromTime, long toTime,
                                        T value, long valueFromTime, long valueToTime
                                        ) {
        if (valueToTime <= fromTime) {
            // ignore before
        } else if (valueFromTime >= toTime) {
            // ignore after
        } else if (fromTime <= valueFromTime && valueToTime <= toTime) {
            // fully included
            addTo(dest, value);
        } else {
            // partially included
            long totalTime = valueToTime - valueFromTime;
            if (totalTime > 0) {
                if (fromTime <= valueFromTime && toTime <= valueToTime) {
                    // partially included start: [accTimeRange.fromTime, toTime]
                    double ratio = (toTime - valueFromTime) / totalTime;
                    addTo(dest, value, ratio);
                } else if (valueFromTime <= fromTime && valueToTime <= toTime) {
                    // partially included end: [fromTime, accTimeRange.toTime]
                    double ratio = (valueToTime - fromTime) / totalTime;
                    addTo(dest, value, ratio);
                } else {
                    // should not occur
                }
            } // else totalTime<=0 should not occur
        }
    }
}


