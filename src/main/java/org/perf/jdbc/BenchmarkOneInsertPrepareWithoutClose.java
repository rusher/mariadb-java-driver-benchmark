
package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BenchmarkOneInsertPrepareWithoutClose extends BenchmarkInit {

    @Benchmark
    public boolean executeText(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionText, state.insertData, state);
    }

    @Benchmark
    public boolean execute(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnection, state.insertData, state);
    }

    @Benchmark
    public boolean prepareAndExecuteComMulti(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionNoCacheMulti, state.insertData, state);
    }


    @Benchmark
    public boolean prepareAndExecute(MyState state) throws Throwable {
        return executeOneInsertPrepare(state.mariadbConnectionNoCache, state.insertData, state);
    }

    private String request = "INSERT INTO blackholeTable (charValue) values (?)";

    public boolean executeOneInsertPrepare(Connection connection, String[] datas, MyState state) throws SQLException {
        state.preparedStatement = connection.prepareStatement(request);
        state.preparedStatement.setString(1, datas[0]);
        return state.preparedStatement.execute();
    }
}
