
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BenchmarkPrepareStatementOneInsertFailover extends BenchmarkInit {
    private String request = "INSERT INTO PerfTextQuery (charValue) values (?)";

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mysqlFailoverConnection, state.insertData);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbFailoverConnection, state.insertData);
    }

    private boolean executeOneInsertPrepare(Connection connection, String[] datas) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, datas[0]);
            return preparedStatement.execute();
        }
    }


}
