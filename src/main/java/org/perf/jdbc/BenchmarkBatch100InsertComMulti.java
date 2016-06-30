
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkBatch100InsertComMulti extends BenchmarkBatch100InsertAbstract {

    @Benchmark
    public int[] mariadb(MyState state) throws Throwable {
        return executeBatch(state.mariadbConnectionComMultiNoCache, state.insertData);
    }

}
