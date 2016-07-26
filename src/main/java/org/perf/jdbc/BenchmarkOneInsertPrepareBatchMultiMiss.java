
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkOneInsertPrepareBatchMultiMiss extends BenchmarkOneInsertPrepareAbstract {

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionBulkNoCache, state.insertData);
    }

}
