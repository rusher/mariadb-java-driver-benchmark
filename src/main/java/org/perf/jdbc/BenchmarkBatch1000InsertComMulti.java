
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkBatch1000InsertComMulti extends BenchmarkBatch1000InsertAbstract {

    @Benchmark
    public int[] mariadb(MyState state) throws Throwable {
        return executeBatch(state.mariadbConnectionComMultiNoCache, state.insertData);
    }

}
