
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkOneInsertPrepareHit extends BenchmarkOneInsertPrepareAbstract {

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mysqlConnection, state.insertData);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnection, state.insertData);
    }

}
