
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.*;

public class BenchmarkCallableStatementWithInParameter extends BenchmarkInit {
    private String request = "{call withResultSet(?)}";
    private int var1 = 1;

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return callableStatementWithInParameter(state.mysqlConnection);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return callableStatementWithInParameter(state.mariadbConnection);
    }

    private boolean callableStatementWithInParameter(Connection connection) throws SQLException {
        try (CallableStatement stmt = connection.prepareCall(request)) {
            stmt.setInt(1, var1);
            return stmt.execute();
        }
    }

}
