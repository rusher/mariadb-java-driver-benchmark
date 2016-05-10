
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BenchmarkOneInsertFailover extends BenchmarkInit {
    private String request = "INSERT INTO PerfTextQuery (charValue, val) values ('abcdefghij0123456', 1)";

    @Benchmark
    public boolean mysql(MyState state) throws SQLException {
        return executeOneInsert(state.mysqlFailoverConnection);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws SQLException {
        return executeOneInsert(state.mariadbFailoverConnection);
    }

    private boolean executeOneInsert(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.execute(request);
        }
    }

}
