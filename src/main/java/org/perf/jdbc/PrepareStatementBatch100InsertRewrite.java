
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class PrepareStatementBatch100InsertRewrite extends PrepareStatementBatch100InsertAbstract {

    @Benchmark
    public int[] mysql(MyState state) throws Throwable {
        return executeBatch(state.mysqlConnectionRewrite, state.insertData);
    }

    @Benchmark
    public int[] mariadb(MyState state) throws Throwable {
        return executeBatch(state.mariadbConnectionRewrite, state.insertData);
    }

}
