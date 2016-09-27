
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class PrepareStatementBatch100InsertPrepareHit extends PrepareStatementBatch100InsertAbstract {

    @Benchmark
    public int[] mysql(MyState state) throws Throwable {
        return executeBatch(state.mysqlConnection, state.insertData);
    }

    @Benchmark
    public int[] mariadb(MyState state) throws Throwable {
        return executeBatch(state.mariadbConnection, state.insertData);
    }

}
