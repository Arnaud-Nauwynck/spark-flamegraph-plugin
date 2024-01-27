
# Launching spark-submit with plugin

```
spark-shell \
    --conf spark.plugins=fr.an.spark.plugin.flamegraph.FlameGraphSparkPlugin \
    --jars "file:///c:/arn/devPerso/my-github/spark-flamegraph-plugin/target/spark-flamegraph-plugin-1.0-SNAPSHOT.jar"
```


# Launching spark-submit with plugin + Debug mode

```
spark-shell \
    --conf spark.plugins=fr.an.spark.plugin.flamegraph.FlameGraphSparkPlugin \
    --jars "file:///c:/arn/devPerso/my-github/spark-flamegraph-plugin/target/spark-flamegraph-plugin-1.0-SNAPSHOT.jar" \
    --conf "spark.driver.extraJavaOptions=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"
```

# Launching spark-submit with plugin + Debug mode + standalone cluster mode

```
spark-shell --master spark://localhost:7077 \
    --conf spark.plugins=fr.an.spark.plugin.flamegraph.FlameGraphSparkPlugin \
    --jars "file:///c:/arn/devPerso/my-github/spark-flamegraph-plugin/target/spark-flamegraph-plugin-1.0-SNAPSHOT.jar" \
    --conf "spark.driver.extraJavaOptions=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000" \
    --conf "spark.executor.extraJavaOptions=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8001"
```

