
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class BenchmarkCallableStatementWithOutParameter extends BenchmarkInit {
    private String request = "{call inOutParam(?)}"; //CREATE PROCEDURE inoutParam(INOUT p1 INT) begin set p1 = p1 + 1; end

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return callableStatementWithOutParameter(state.mysqlConnection, state);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return callableStatementWithOutParameter(state.mariadbConnection, state);
    }

    private String callableStatementWithOutParameter(Connection connection, MyState state) throws SQLException {
        try (CallableStatement storedProc = connection.prepareCall(request)) {
            storedProc.setInt(1, state.functionVar1); //2
            storedProc.registerOutParameter(1, Types.INTEGER);
            storedProc.execute();
            return storedProc.getString(1);
        }
    }

}
