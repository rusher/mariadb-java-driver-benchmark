
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class BenchmarkCallableStatementFunction extends BenchmarkInit {
    private String request = "{? = CALL testFunctionCall(?,?,?)}"; //CREATE FUNCTION testFunctionCall(a float, b bigint, c int) RETURNS INT NO SQL BEGIN \nRETURN a; \nEND

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return callableStatementFunction(state.mysqlConnection, state);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return callableStatementFunction(state.mariadbConnection, state);
    }

    private boolean callableStatementFunction(Connection connection, MyState state) throws SQLException {
        try (CallableStatement callableStatement = connection.prepareCall(request)) {
            callableStatement.registerOutParameter(1, Types.INTEGER);
            callableStatement.setFloat(2, state.functionVar1); //2
            callableStatement.setInt(3, state.functionVar2); //1
            callableStatement.setInt(4, state.functionVar3); //1
            return callableStatement.execute();
        }
    }


}
