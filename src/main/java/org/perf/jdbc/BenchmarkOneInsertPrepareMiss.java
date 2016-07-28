
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkOneInsertPrepareMiss extends BenchmarkOneInsertPrepareAbstract {

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mysqlConnectionNoCache, state.insertData);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionNoCache, state.insertData);
    }

    @Benchmark
    public boolean mariadbWithout102capacity(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionBulkNoCache, state.insertData);
    }
}
