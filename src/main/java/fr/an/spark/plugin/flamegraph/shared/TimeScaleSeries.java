package fr.an.spark.plugin.flamegraph.shared;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.Objects;

/**
 *
 * @param <T>
 */
public class TimeScaleSeries<T> {

    private final SumScaleValueSupport<T> ops;

    private final AccTimeScaleEntry<T>[] scaleEntries;

    private AccTimeRange<T> remainScaleValue;

    static class AccTimeScaleEntry<T> {
        final int scaleIndex; // implicit
        final int length; // = accTimeRanges.length
        int currentCount;
        int currentFirstIndexModulo;
        /** last index, inclusive */
        int currentLastIndexModulo;

        final AccTimeRange<T>[] accTimeRanges;

        AccTimeScaleEntry(int scaleIndex, int length) {
            this.scaleIndex = scaleIndex;
            this.length = length;
            this.accTimeRanges = new AccTimeRange[length];
        }

        public AccTimeRange<T> shiftAdd(TimeScaleSeries<T> parent, AccTimeRange<T> accTimeRange) {
            AccTimeRange<T> carry = null;
            currentCount++;
            if (currentCount > length) {
                // remove last 2, sum and return carry to next
                AccTimeRange<T> last = accTimeRanges[currentLastIndexModulo];
                currentLastIndexModulo--;
                if (currentLastIndexModulo < 0) {
                    currentLastIndexModulo = length-1;
                }
                AccTimeRange<T> prevLast = accTimeRanges[currentLastIndexModulo];
                currentLastIndexModulo--;
                if (currentLastIndexModulo < 0) {
                    currentLastIndexModulo = length-1;
                }
                carry = parent.sumTimeRange(prevLast, last);
                currentCount-=2;
            }
            currentFirstIndexModulo--;
            if (currentFirstIndexModulo < 0) {
                currentFirstIndexModulo = length-1;
            }
            accTimeRanges[currentFirstIndexModulo] = accTimeRange;
            return carry;
        }
    }

    @RequiredArgsConstructor
    public static class AccTimeRange<T> {
        public final long fromTime;
        public final long toTime;
        public final T accValue;

    }

    //---------------------------------------------------------------------------------------------

    public TimeScaleSeries(int[] scaleLengths,
                           SumScaleValueSupport<T> ops) {
        this.ops = Objects.requireNonNull(ops);
        this.scaleEntries = new AccTimeScaleEntry[scaleLengths.length];
        for(int i = 0; i < scaleLengths.length; i++) {
            this.scaleEntries[i] = new AccTimeScaleEntry(i, scaleLengths[i]);
        }
    }

    //---------------------------------------------------------------------------------------------

    public void shiftAdd(AccTimeRange<T> value) {
        int currScale = 0;
        AccTimeRange<T> currValue = value;
        for(;;) {
            AccTimeRange<T> carry = scaleEntries[currScale].shiftAdd(this, currValue);
            if (carry == null) {
                break;
            }
            currScale++;
            currValue = carry;
            if (currScale >= scaleEntries.length) {
                if (remainScaleValue != null) {
                    // add to last
                    this.remainScaleValue = sumTimeRange(remainScaleValue, carry);
                } else {
                    this.remainScaleValue = carry;
                }
                break;
            }
        }
    }

    private AccTimeRange<T> sumTimeRange(AccTimeRange<T> first, AccTimeRange<T> second) {
        T sumValue = ops.sum(first.accValue, second.accValue);
        return new AccTimeRange<T>(first.fromTime, second.toTime, sumValue);
    }

    public <TAcc> TAcc extractTimeRange(long fromTime, long toTime, SumScaleValueAccSupport<T,TAcc> accSupport) {
        TAcc res = accSupport.createAcc();
        for(int i = 0; i < scaleEntries.length; i++) {
            val scaleEntry = scaleEntries[i];

            final int scaleEntryLength = scaleEntry.length;
            final int scaleLastIndex = scaleEntry.currentLastIndexModulo;
            for(int j = scaleEntry.currentFirstIndexModulo; j < scaleLastIndex; j = (j+1)%scaleEntryLength) {
                AccTimeRange<T> accTimeRange = scaleEntry.accTimeRanges[j];
                accSupport.addTimeRangeIntersectTo(res, fromTime, toTime,
                        accTimeRange.accValue, accTimeRange.fromTime, accTimeRange.toTime);
                // TOADD optim for loop?
            } // for j
        } // for i

        if (remainScaleValue != null) {
            accSupport.addTimeRangeIntersectTo(res, fromTime, toTime,
                    remainScaleValue.accValue, remainScaleValue.fromTime, remainScaleValue.toTime);
        }
        return res;
    }

}
