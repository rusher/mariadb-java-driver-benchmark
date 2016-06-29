package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkSelect1RowPrepareRewrite extends BenchmarkSelect1RowPrepareAbstract {

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1RowPrepare(state.mysqlConnectionRewrite, state);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnectionRewrite, state);
    }

}
