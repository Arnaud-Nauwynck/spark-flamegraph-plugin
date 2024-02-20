package fr.an.spark.plugin.flamegraph.shared.stacktrace;

import lombok.Value;

import java.io.Serializable;
import java.lang.management.LockInfo;

@Value
public class StackTraceElementKey implements Serializable {

    public final String className;
    public final String methodName;
    public final String fileName;
    public final int lineNumber;

    // only for top-level stack element, if LockInfo != null
    public final String lockThreadState;

    //---------------------------------------------------------------------------------------------

    public static StackTraceElementKey createFrom(StackTraceElement ste) {
        return new StackTraceElementKey(ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber(), null);
    }

    public static StackTraceElementKey createFrom0(StackTraceElement ste, Thread.State ts, LockInfo lockInfo) {
        String lockThreadState = null;
        if (lockInfo != null) {
            String lockClassName = lockInfo.getClassName();
            switch (ts) {
                case BLOCKED:
                    lockThreadState = "blocked on " + lockClassName;
                    break;
                case WAITING:
                    lockThreadState = "waiting on " + lockClassName;
                    break;
                case TIMED_WAITING:
                    lockThreadState = "timedwaiting on " + lockClassName;
                    break;
                default:
                    lockThreadState = null;
                    break;
            }
        }
        return new StackTraceElementKey(ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber(), lockThreadState);
    }

    public String toName() {
        String res = className + "." + methodName;
        if (fileName != null) {
            res += "(" + fileName + ":" + lineNumber + ")";
        }
        return res;
    }

}
