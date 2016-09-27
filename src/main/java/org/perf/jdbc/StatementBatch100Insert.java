
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class StatementBatch100Insert extends BenchmarkInit {

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

    public int[] executeBatch(Connection connection, String[] data) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (int i = 0; i < 100; i++) {
                statement.addBatch("INSERT INTO blackholeTable (charValue) values ( '" + data[i].replace("\\", "\\\\").replace("'", "\\'") + "')");
            }
            return statement.executeBatch();
        }
    }

}
