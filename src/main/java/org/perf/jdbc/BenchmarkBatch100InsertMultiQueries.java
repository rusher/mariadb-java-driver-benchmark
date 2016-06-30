
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkBatch100InsertMultiQueries extends BenchmarkBatch100InsertAbstract {

    @Benchmark
    public int[] mariadb(MyState state) throws Throwable {
        return executeBatch(state.mariadbConnectionAllowMultiQueries, state.insertData);
    }

}
