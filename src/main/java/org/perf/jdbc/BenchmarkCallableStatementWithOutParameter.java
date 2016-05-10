
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.*;

public class BenchmarkCallableStatementWithOutParameter extends BenchmarkInit {
    private String request = "{call inOutParam(?)}";

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return callableStatementWithOutParameter(state.mysqlConnection);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return callableStatementWithOutParameter(state.mariadbConnection);
    }

    private String callableStatementWithOutParameter(Connection connection) throws SQLException {
        try (CallableStatement storedProc = connection.prepareCall(request)) {
            storedProc.setInt(1, 1);
            storedProc.registerOutParameter(1, Types.INTEGER);
            storedProc.execute();
            return storedProc.getString(1);
        }
    }

}
