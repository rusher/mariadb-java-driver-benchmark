package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkSelect1RowPrepareMiss extends BenchmarkSelect1RowPrepareAbstract {

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1RowPrepare(state.mysqlConnectionNoCache, state);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnectionNoCache, state);
    }

    @Benchmark
    public String mariadbWithout102capacity(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnectionBulkNoCache, state);
    }

}
