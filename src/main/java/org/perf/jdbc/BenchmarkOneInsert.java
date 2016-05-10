
package org.perf.jdbc;

import org.openjdk.jmh.annotations.*;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public class BenchmarkOneInsert extends BenchmarkInit {
    private String request = "INSERT INTO PerfTextQuery (charValue, val) values ('abcdefghij0123456', 1)";

    @Benchmark
    public boolean mysql(MyState state) throws SQLException {
        return executeOneInsert(state.mysqlConnection);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws SQLException {
        return executeOneInsert(state.mariadbConnection);
    }

    @Benchmark
    public boolean drizzle(MyState state) throws SQLException {
        return executeOneInsert(state.drizzleConnectionText);
    }

    private boolean executeOneInsert(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.execute(request);
        }
    }

}
