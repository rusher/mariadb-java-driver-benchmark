
package org.perf.jdbc;

import org.openjdk.jmh.annotations.*;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class BenchmarkCallableStatementFunction extends BenchmarkInit {
    private String request = "{? = CALL testFunctionCall(?,?,?)}";

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return callableStatementFunction(state.mysqlConnection);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return callableStatementFunction(state.mariadbConnection);
    }

    private boolean callableStatementFunction(Connection connection) throws SQLException {
        try (CallableStatement callableStatement = connection.prepareCall(request)) {
            callableStatement.registerOutParameter(1, Types.INTEGER);
            callableStatement.setFloat(2, 2);
            callableStatement.setInt(3, 1);
            callableStatement.setInt(4, 1);
            return callableStatement.execute();
        }
    }


}
