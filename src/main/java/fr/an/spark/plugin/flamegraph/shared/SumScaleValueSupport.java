package fr.an.spark.plugin.flamegraph.shared;

public abstract class SumScaleValueSupport<T> {

    public abstract T sum(T left, T right);
    public abstract T scalar(T src, double coef);

}


