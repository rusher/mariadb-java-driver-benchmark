
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkOneInsertPrepareTextHA extends BenchmarkOneInsertPrepareAbstract {

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mysqlFailoverConnection, state.insertData);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbFailoverConnection, state.insertData);
    }

}
