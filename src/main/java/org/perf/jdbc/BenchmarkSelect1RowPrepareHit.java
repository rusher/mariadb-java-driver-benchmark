package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.*;

public class BenchmarkSelect1RowPrepareHit extends BenchmarkInit {
    private String request = "SELECT * FROM PerfReadQuery where charValue = ? or id = ?";
    private String var = "abc";
    private int counter = 0;

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1RowPrepare(state.mysqlConnection);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1RowPrepare(state.mariadbConnection);
    }

    private String select1RowPrepare(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(request)) {
            preparedStatement.setString(1, var + counter % 1000);
            preparedStatement.setInt(2, counter % 1000);
            counter++;
            try (ResultSet rs = preparedStatement.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
