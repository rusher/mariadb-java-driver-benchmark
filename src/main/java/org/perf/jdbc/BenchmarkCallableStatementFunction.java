
package org.perf.jdbc;

import org.openjdk.jmh.annotations.*;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.*;
import java.util.concurrent.TimeUnit;

public class BenchmarkCallableStatementFunction extends BenchmarkInit {
    private String request = "{? = CALL testFunctionCall(?,?,?)}";
    private int var1 = 2;
    private int var2 = 1;
    private int var3 = 1;

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
            callableStatement.setFloat(2, var1);
            callableStatement.setInt(3, var2);
            callableStatement.setInt(4, var3);
            return callableStatement.execute();
        }
    }


}
