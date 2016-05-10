package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BenchmarkSelect1Row extends BenchmarkInit {
    private String request = "SELECT * FROM PerfReadQuery where charValue = 'abc0'";

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1Row(state.mysqlConnection);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1Row(state.mariadbConnection);
    }

    @Benchmark
    public String drizzle(MyState state) throws Throwable {
        return select1Row(state.drizzleConnectionText);
    }

    private String select1Row(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(request)) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
