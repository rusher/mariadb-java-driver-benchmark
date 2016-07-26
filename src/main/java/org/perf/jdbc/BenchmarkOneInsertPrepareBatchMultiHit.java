
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkOneInsertPrepareBatchMultiHit extends BenchmarkOneInsertPrepareAbstract {

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionBulkCache, state.insertData);
    }

}
