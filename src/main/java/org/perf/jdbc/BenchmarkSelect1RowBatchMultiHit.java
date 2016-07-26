package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkSelect1RowBatchMultiHit extends BenchmarkSelect1RowPrepareAbstract {

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnectionBulkCache, state);
    }

}
