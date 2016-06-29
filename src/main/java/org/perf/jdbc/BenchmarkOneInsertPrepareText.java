
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkOneInsertPrepareText extends BenchmarkOneInsertPrepareAbstract {

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mysqlConnectionText, state.insertData);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionText, state.insertData);
    }

    @Benchmark
    public boolean drizzle(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.drizzleConnectionText, state.insertData);
    }

}
