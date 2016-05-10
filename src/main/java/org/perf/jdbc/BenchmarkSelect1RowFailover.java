package org.perf.jdbc;

import org.openjdk.jmh.annotations.Benchmark;
import org.perf.jdbc.common.BenchmarkInit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BenchmarkSelect1RowFailover extends BenchmarkInit {
    private int counter = 0;

    private String getRequest() {
        int counterMod = counter++ % 1000;
        return "SELECT * FROM PerfReadQuery where charValue = 'abc" + counterMod + "' or id = " + counterMod;
    }

    @Benchmark
    public String mysql(MyState state) throws Throwable {
        return select1Row(state.mysqlFailoverConnection);
    }

    @Benchmark
    public String mariadb(MyState state) throws Throwable {
        return select1Row(state.mariadbFailoverConnection);
    }

    private String select1Row(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(getRequest())) {
                rs.next();
                return rs.getString(1);
            }
        }
    }
}
