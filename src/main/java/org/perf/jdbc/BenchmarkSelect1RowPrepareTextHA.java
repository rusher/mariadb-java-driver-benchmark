package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkSelect1RowPrepareTextHA extends BenchmarkSelect1RowPrepareAbstract {

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1RowPrepare(state.mysqlFailoverConnection, state);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbFailoverConnection, state);
    }

}
