
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;

public class BenchmarkBatch100InsertText extends BenchmarkBatch100InsertAbstract {

    @Benchmark
    public int[] mysql(MyState state) throws Throwable {
        return executeBatch(state.mysqlConnectionText, state.insertData);
    }

    @Benchmark
    public int[] mariadb(MyState state) throws Throwable {
        return executeBatch(state.mariadbConnectionText, state.insertData);
    }

    @Benchmark
    public int[] drizzle(MyState state) throws Throwable {
        return executeBatch(state.drizzleConnectionText, state.insertData);
    }

}
