package fr.an.spark.plugin.flamegraph.shared.stacktrace;

import fr.an.spark.plugin.flamegraph.driver.rest.dto.StackTraceEntryDTO;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesRequest.ResolveStackTraceRequest;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesResponse;
import fr.an.spark.plugin.flamegraph.shared.protocol.ResolveStackTracesResponse.ResolveStackTraceResponse;
import fr.an.spark.plugin.flamegraph.shared.utils.LsUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StackTraceEntryRegistry {

    private int idGenerator = 2;
    private final Map<Integer,StackTraceEntry> byId = new HashMap<>();

    @Getter
    private final StackTraceEntry rootEntry = new StackTraceEntry(1, null, null);

    //---------------------------------------------------------------------------------------------

    public StackTraceEntryRegistry() {
        byId.put(rootEntry.id, rootEntry);
    }

    //---------------------------------------------------------------------------------------------


    public List<StackTraceEntryDTO> listStackRegistryEntries() {
        return LsUtils.map(byId.values(), StackTraceEntry::toDTO);
    }

    public StackTraceEntry findById(int id) {
        return byId.get(id);
    }

    // processed on driver side
    public ResolveStackTracesResponse handleResolveStackTracesRequest(ResolveStackTracesRequest req) {
        val resolvedStackTraces = new ArrayList<ResolveStackTraceResponse>(req.toResolve.size());
        for(val toResolve: req.toResolve) {
            resolvedStackTraces.add(handleResolveStackTraceRequest(toResolve));
        }
        return new ResolveStackTracesResponse(resolvedStackTraces);
    }

    private ResolveStackTraceResponse handleResolveStackTraceRequest(ResolveStackTraceRequest req) {
        int fromEntryId = req.fromEntryId;
        StackTraceEntry stackTraceEntry = byId.get(fromEntryId);
        if (stackTraceEntry == null) {
            log.error("should not occur: StackTrace Entry not found by id:" + fromEntryId);
            return null;
        }
        StackTraceEntry curr = stackTraceEntry;
        int remainLen = req.remainElements.length;
        int[] remainElementIds = new int[remainLen];
        for(int i = remainLen-1; i>=0; i--) {
            val remainElementKey = req.remainElements[i];
            StackTraceEntry remainEntry = curr.findChild(remainElementKey);
            if (remainEntry == null) {
                int id = idGenerator++;
                remainEntry = new StackTraceEntry(id, curr, remainElementKey);
                byId.put(id, remainEntry);
                curr._registerChild(remainEntry);
            }
            remainElementIds[i] = remainEntry.id;
            curr = remainEntry;
        }
        return new ResolveStackTraceResponse(fromEntryId, remainElementIds);
    }


    // processed on executor side
    public void registerResolveResponse(
            ResolveStackTracesRequest resolveRequest,
            ResolveStackTracesResponse resp) {
        int resolveLen = resolveRequest.toResolve.size();
        if (resp.resolvedStackTraces.size() != resolveLen) {
            log.error("should not occur");
            return;
        }
        for (int i = 0; i < resolveLen; i++) {
            val toResolveStack = resolveRequest.toResolve.get(i);
            val resolvedStack = resp.resolvedStackTraces.get(i);

            int fromEntryId = toResolveStack.fromEntryId;
            StackTraceElementKey[] remainElements = toResolveStack.remainElements;

            if (resolvedStack == null) {
                log.error("should not occur: resp.resolvedStackTraces.get(" + i + ") : null");
                continue;
            }
            int[] remainElementIds = resolvedStack.remainElementIds;
            int remainLen = remainElements.length;
            if (remainElementIds.length != remainLen) {
                log.error("should not occur: remainElementIds.length: " + remainElementIds.length + " != resolvedStack.remainElementIds:" + resolvedStack.remainElementIds);
                continue;
            }
            registerResolvedEntryIds(fromEntryId, remainElements, remainElementIds);
        }
    }

    public void registerResolvedEntryIds(int fromEntryId,
                                         StackTraceElementKey[] remainElements,
                                         int[] remainElementIds) {
        int currId = fromEntryId;
        StackTraceEntry curr = byId.get(fromEntryId);
        int remainLen = remainElements.length;
        for(int i = remainLen-1; i>=0; i--) {
            val childKey = remainElements[i];
            val childId = remainElementIds[i];
            StackTraceEntry foundChild = byId.get(childId);
            if (foundChild == null) {
                // register new child
                foundChild = new StackTraceEntry(childId, curr, childKey);
                byId.put(childId, foundChild);
                curr._registerChild(foundChild);
            } else {
                // already registered.. ignore
            }
            curr = foundChild;
        }
    }

    @RequiredArgsConstructor
    public static class PartiallyResolvedStackTrace {
        public final int resolvedUpTo;
        public final StackTraceEntry resolvedEntry;
        public final StackTraceElementKey[] remainElementKeys;
    }

    public PartiallyResolvedStackTrace partialResolveStackTrace(ThreadInfo threadInfo) {
        val stackTrace = threadInfo.getStackTrace();
        final int len = stackTrace.length;
        StackTraceEntry curr = rootEntry;
        for(int i = len-1; i >= 0; i--) {
            val key = (i != 0)? StackTraceElementKey.createFrom(stackTrace[i])
                    : StackTraceElementKey.createFrom0(stackTrace[i], threadInfo.getThreadState(), threadInfo.getLockInfo());
            StackTraceEntry foundChildEntry = curr.findChild(key);
            if (foundChildEntry == null) {
                int remainLen = i+1;
                val remainElts = new StackTraceElementKey[remainLen];
                int remainOffset = len-1-i;
                for(int r = 0; r < remainLen; r++) {
                    remainElts[r] = StackTraceElementKey.createFrom(stackTrace[remainOffset + r]);
                }
                return new PartiallyResolvedStackTrace(remainOffset, curr, remainElts);
            } else {
                curr = foundChildEntry;
            }
        }
        return new PartiallyResolvedStackTrace(len, curr, null);
    }

    public PartiallyResolvedStackTrace redoResolveStackTrace(
            PartiallyResolvedStackTrace partiallyResolvedStackTrace,
            ThreadInfo threadInfo // unused
    ) {
        StackTraceEntry curr = partiallyResolvedStackTrace.resolvedEntry;
        val remainKeys = partiallyResolvedStackTrace.remainElementKeys;
        int remainLen = remainKeys.length;
        int resolveUpTo = partiallyResolvedStackTrace.resolvedUpTo + remainLen;
        for (int i = remainLen-1; i >= 0; i--) {
            val next = curr.findChild(remainKeys[i]);
            if (next == null) {
                log.warn("should not occur..  redoResolveStackTrace => null");
                resolveUpTo = partiallyResolvedStackTrace.resolvedUpTo + remainLen-1 - i;
                break;
            }
            curr = next;
        }
        return new PartiallyResolvedStackTrace(resolveUpTo, curr, null);
    }

}
