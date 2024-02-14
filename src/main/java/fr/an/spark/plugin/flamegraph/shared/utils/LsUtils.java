package fr.an.spark.plugin.flamegraph.shared.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class LsUtils {

    public static <TSrc,TDest> List<TDest> map(Collection<TSrc> src, Function<TSrc,TDest> mapFunc) {
        List<TDest> res = new ArrayList<>(src.size());
        for(TSrc srcElt : src) {
            res.add(mapFunc.apply(srcElt));
        }
        return res;
    }

    public static <TSrc,TDest> List<TDest> flatMapNonNull(Collection<TSrc> src, Function<TSrc,TDest> mapFunc) {
        List<TDest> res = new ArrayList<>(src.size());
        for(TSrc srcElt : src) {
            TDest optResElt = mapFunc.apply(srcElt);
            if (optResElt != null) {
                res.add(optResElt);
            }
        }
        return res;
    }

}
