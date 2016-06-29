
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkOneInsertPrepareComMultiMiss extends BenchmarkOneInsertPrepareAbstract {

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionComMultiNoCache, state.insertData);
    }

}
