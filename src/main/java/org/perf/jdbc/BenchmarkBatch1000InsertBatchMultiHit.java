
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkBatch1000InsertBatchMultiHit extends BenchmarkBatch1000InsertAbstract {

    @Benchmark
    public int[] mariadb(MyState state) throws Throwable {
        return executeBatch(state.mariadbConnectionBulkCache, state.insertData);
    }

}
