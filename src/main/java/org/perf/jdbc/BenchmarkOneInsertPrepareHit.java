
package org.perf.jdbc;

import org.openjdk.jmh.annotations.*;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class BenchmarkOneInsertPrepareHit extends BenchmarkInit {
    private String request = "INSERT INTO PerfTextQuery (charValue, val) values (?, ?)";
    private int counter = 0;

    @Benchmark
    public boolean mysql(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mysqlConnection, state.insertData);
    }

    @Benchmark
    public boolean mariadb(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnection, state.insertData);
    }

    private boolean executeOneInsertPrepare(Connection connection, String[] datas) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, datas[counter % 1000]);
            preparedStatement.setInt(2, counter++);
            return preparedStatement.execute();
        }
    }
}
