package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkSelect1RowPrepareText extends BenchmarkSelect1RowPrepareAbstract {

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1RowPrepare(state.mysqlConnectionText, state);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnectionText, state);
    }

    @Benchmark
    public String drizzle(MyState state) throws Throwable {
        return select1RowPrepare(state.drizzleConnectionText, state);
    }

}
